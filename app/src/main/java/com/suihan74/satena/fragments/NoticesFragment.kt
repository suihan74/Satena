package com.suihan74.satena.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.Notice
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.adapters.NoticesAdapter
import com.suihan74.satena.R
import com.suihan74.satena.activities.ActivityBase
import com.suihan74.satena.models.NoticesKey
import com.suihan74.utilities.CoroutineScopeFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime

class NoticesFragment : CoroutineScopeFragment() {
    private var mClickHandling = false

    companion object {
        fun createInstance() : NoticesFragment = NoticesFragment().apply {
            enterTransition = TransitionSet()
                .addTransition(Fade())
                .addTransition(Slide(Gravity.TOP))
        }

        fun createMessage(notice: Notice, context: Context) : String {
            val nameColor = ContextCompat.getColor(context, R.color.colorPrimary)
            val starColor = ContextCompat.getColor(context, R.color.starYellow)

            val comment = (notice.metadata?.subjectTitle ?: "").toCharArray()
            val sourceComment = comment.joinToString(
                separator = "",
                limit = 9,
                truncated = "..."
            )

            val users = notice.objects
                .groupBy { it.user }
                .map { it.value.first() }
                .reversed()
                .joinToString(
                    separator = "、",
                    limit = 3,
                    truncated = "ほか${notice.objects.count() - 3}人",
                    transform = { "<font color=\"$nameColor\">${it.user}</font>さん" })

            return when (notice.verb) {
                Notice.VERB_STAR ->
                    "${users}があなたのブコメ($sourceComment)に<font color=\"$starColor\">★</font>をつけました"

                Notice.VERB_ADD_FAVORITE ->
                    "${users}があなたのブックマークをお気に入りに追加しました"

                else -> throw NotImplementedError("verb: ${notice.verb}")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notices, container, false)

        val activity = activity as ActivityBase
        activity.showProgressBar()

        // ツールバーの設定
        val toolbar = view.findViewById<Toolbar>(R.id.notices_toolbar)
        toolbar.title = "通知 > " + HatenaClient.account!!.name

        val noticesAdapter = object : NoticesAdapter() {
            override fun onItemClicked(notice: Notice) {
                if (mClickHandling) return
                mClickHandling = true

                when (notice.verb) {
                    Notice.VERB_STAR ->
                        launch(Dispatchers.Main) {
                            try {
                                activity.showProgressBar()

                                val entry = HatenaClient.getBookmarksEntryAsync(notice.eid).await()

                                val intent = Intent(activity, BookmarksActivity::class.java).apply {
                                    putExtra("entry", entry)
                                    putExtra("target_user", HatenaClient.account!!.name)
                                }
                                startActivity(intent)
                            }
                            catch (e: Exception) {
                                Log.d("FailedToFetchComment", e.message)
                                activity.showToast("通知対象ブコメの取得失敗")
                                mClickHandling = false
                            }
                            finally {
                                activity.hideProgressBar()
                            }
                        }

                    Notice.VERB_ADD_FAVORITE -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.link))
                        startActivity(intent)
                        mClickHandling = false
                    }
                }
            }
        }

        view.findViewById<RecyclerView>(R.id.notices_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
                R.drawable.recycler_view_item_divider
            )!!)
            addItemDecoration(dividerItemDecoration)
            layoutManager = LinearLayoutManager(context)
            adapter = noticesAdapter
        }

        launch(Dispatchers.Main) {
            try {
                val prefs = SafeSharedPreferences.create<PreferenceKey>(context)

                // 通知の既読状態を更新
                val lastSeenUpdatable = prefs.getBoolean(PreferenceKey.NOTICES_LAST_SEEN_UPDATABLE)
                if (lastSeenUpdatable) {
                    try {
                        HatenaClient.updateNoticesLastSeenAsync().await()
                    }
                    catch (e: Exception) {
                        Log.d("FailedToUpdateLastSeen", e.message)
                    }
                }
                prefs.edit {
                    putObject(PreferenceKey.NOTICES_LAST_SEEN, LocalDateTime.now())
                }

                val noticesPrefs = SafeSharedPreferences.create<NoticesKey>(context)
                val savedNotices = noticesPrefs.get<List<Notice>>(NoticesKey.NOTICES)
                val noticesSize = noticesPrefs.getInt(NoticesKey.NOTICES_SIZE)

                val response = HatenaClient.getNoticesAsync().await()
                val fetchedNotices = response.notices

                val oldNotices = savedNotices.filterNot { existed -> fetchedNotices.any { newer ->
                    newer.created == existed.created
                } }

                val notices = oldNotices.plus(fetchedNotices)
                    .sortedByDescending { it.modified }
                    .take(noticesSize)

                noticesPrefs.edit {
                    put(NoticesKey.NOTICES, notices)
                }

                noticesAdapter.setNotices(notices)
            }
            catch (e: Exception) {
                Log.d("FailedToFetchNotices", Log.getStackTraceString(e))
                context?.showToast("通知リスト取得失敗")
            }
            finally {
                activity.hideProgressBar()
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        mClickHandling = false
    }
}
