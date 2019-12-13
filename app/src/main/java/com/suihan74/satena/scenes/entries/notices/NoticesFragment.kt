package com.suihan74.satena.scenes.entries.notices

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.Notice
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.ReportDialogFragment
import com.suihan74.satena.models.NoticeTimestamp
import com.suihan74.satena.models.NoticesKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.utilities.CoroutineScopeFragment
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime

class NoticesFragment : CoroutineScopeFragment(), AlertDialogFragment.Listener {
    private var mClickHandling = false
    private lateinit var mNoticesAdapter: NoticesAdapter

    override val title: String
        get() = "通知 > ${HatenaClient.account?.name ?: "?"}"

    companion object {
        fun createInstance() : NoticesFragment =
            NoticesFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.TOP))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notices, container, false)

        val activity = activity as ActivityBase
        activity.showProgressBar()

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
                                    putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
                                    putExtra(BookmarksActivity.EXTRA_TARGET_USER, HatenaClient.account!!.name)
                                }
                                startActivity(intent)
                            }
                            catch (e: Exception) {
                                Log.d("FailedToFetchComment", e.message)
                                activity.showToast("通知対象ブコメの取得失敗")
                            }
                            finally {
                                activity.hideProgressBar()
                                mClickHandling = false
                            }
                        }

                    Notice.VERB_ADD_FAVORITE -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.link))
                        startActivity(intent)
                        mClickHandling = false
                    }

                    Notice.VERB_BOOKMARK -> {
                        val baseUrl = "${HatenaClient.B_BASE_URL}/entry?url="
                        if (notice.link.startsWith(baseUrl)) {
                            val url = Uri.decode(notice.link.substring(baseUrl.length))
                            launch(Dispatchers.Main) {
                                try {
                                    activity.showProgressBar()

                                    val entry = HatenaClient.getBookmarksEntryAsync(url).await()

                                    val intent =
                                        Intent(activity, BookmarksActivity::class.java).apply {
                                            putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
                                        }
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    Log.d("FailedToFetchComment", e.message)
                                    activity.showToast("通知対象ブコメの取得失敗")
                                } finally {
                                    activity.hideProgressBar()
                                    mClickHandling = false
                                }
                            }
                        }
                        else {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.link))
                            startActivity(intent)
                            mClickHandling = false
                        }
                    }

                    else -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.link))
                        startActivity(intent)
                        mClickHandling = false
                    }
                }
            }

            override fun onItemLongClicked(notice: Notice): Boolean {
                val items = notice.users
                    .map { getString(R.string.menu_notice_report, it) }
                    .plus(getString(R.string.menu_notice_remove))

                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setItems(items)
                    .setAdditionalData("notice", notice)
                    .show(childFragmentManager, "notice_menu_dialog")

                return true
            }
        }
        mNoticesAdapter = noticesAdapter

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
                val removedNotices = noticesPrefs.get<List<NoticeTimestamp>>(NoticesKey.REMOVED_NOTICE_TIMESTAMPS)

                val response = HatenaClient.getNoticesAsync().await()
                val fetchedNotices = response.notices

                val oldNotices = savedNotices.filterNot { existed -> fetchedNotices.any { newer ->
                    newer.created == existed.created
                } }

                var oldestMatched = LocalDateTime.MAX
                val notices = oldNotices.plus(fetchedNotices)
                    .filterNot { n ->
                        removedNotices.any { it.created == n.created && it.modified == n.modified }.also { result ->
                            if (result) {
                                oldestMatched = minOf(n.modified, oldestMatched)
                            }
                        }
                    }
                    .sortedByDescending { it.modified }
                    .take(noticesSize)

                noticesPrefs.edit {
                    put(NoticesKey.NOTICES, notices)

                    // 古い削除済み通知設定を消去する
                    if (oldestMatched < LocalDateTime.MAX) {
                        put(NoticesKey.REMOVED_NOTICE_TIMESTAMPS, removedNotices.filter {
                            it.modified >= oldestMatched
                        })
                    }
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

    private fun removeNotice(notice: Notice) {
        val prefs = SafeSharedPreferences.create<NoticesKey>(context)
        val removedNotices = prefs.get<List<NoticeTimestamp>>(NoticesKey.REMOVED_NOTICE_TIMESTAMPS)
            .plus(NoticeTimestamp(notice.created, notice.modified))

        prefs.edit {
            put(NoticesKey.REMOVED_NOTICE_TIMESTAMPS, removedNotices)
        }

        mNoticesAdapter.removeNotice(notice)
    }

    private fun reportUser(user: String) {
        val dialog = ReportDialogFragment.createInstance(user)
        dialog.show(fragmentManager!!, "report_dialog")
    }

    override fun onSelectItem(dialog: AlertDialogFragment, which: Int) {
        val notice = dialog.getAdditionalData<Notice>("notice")!!
        val items = dialog.items!!

        when {
            items[which] == getString(R.string.menu_notice_remove) -> {
                this@NoticesFragment.removeNotice(notice)
            }

            else -> {
                val user = notice.users.firstOrNull { getString(R.string.menu_notice_report, it) == items[which] } ?: return
                reportUser(user)
            }
        }
    }
}
