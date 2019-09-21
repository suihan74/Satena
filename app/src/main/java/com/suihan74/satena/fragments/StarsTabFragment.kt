package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.Star
import com.suihan74.satena.R
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.adapters.StarsAdapter
import com.suihan74.utilities.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class StarsTabFragment : CoroutineScopeFragment() {
    private lateinit var mRoot: View
    private var mBookmarksFragment : BookmarksFragment? = null
    private var mBookmarkDetailFragment : BookmarkDetailFragment? = null

    private lateinit var mBookmark : Bookmark
    private lateinit var mStarsTabMode : StarsAdapter.StarsTabMode

    private lateinit var mStarsAdapter: StarsAdapter

    companion object {
        fun createInstance(
            b: Bookmark,
            tabMode: StarsAdapter.StarsTabMode
        ) = StarsTabFragment().apply {
            mBookmark = b
            mStarsTabMode = tabMode
        }

        private const val BUNDLE_BOOKMARK_USER = "mBookmark.user"
        private const val BUNDLE_STARS_TAB_MODE = "mStarsTabMode"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putString(BUNDLE_BOOKMARK_USER, mBookmark.user)
            putInt(BUNDLE_STARS_TAB_MODE, mStarsTabMode.int)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_stars_tab, container, false)
        mRoot = view

        val activity = activity as? BookmarksActivity ?: throw IllegalStateException("StarsTabFragment has created from an invalid activity")

        savedInstanceState?.let {
            val user = it.getString(BUNDLE_BOOKMARK_USER)
            mStarsTabMode = StarsAdapter.StarsTabMode.fromInt(it.getInt(BUNDLE_STARS_TAB_MODE))
            mBookmark = activity.getStackedFragment<BookmarkDetailFragment> { it.bookmark.user == user }!!.bookmark
        }

        mBookmarksFragment = activity.bookmarksFragment
        mBookmarkDetailFragment = activity.getStackedFragment { it?.bookmark?.user == mBookmark.user }

        val bookmarksEntry = mBookmarksFragment!!.bookmarksEntry!!
        val starsMap = mBookmarksFragment!!.starsMap
        val allBookmarks = bookmarksEntry.bookmarks

        // initialize bookmarks list
        view.findViewById<RecyclerView>(R.id.stars_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!, R.drawable.recycler_view_item_divider)!!)

            addItemDecoration(dividerItemDecoration)
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            mStarsAdapter = object : StarsAdapter(context, mBookmark, starsMap, allBookmarks, mStarsTabMode) {
                override fun removeItem(user: String) {
                    super.removeItem(user)
                    mBookmarkDetailFragment?.updateStars()
                }

                override fun onItemClicked(user: String, star: Star?) {
                    val target = allBookmarks.firstOrNull { it.user == user } ?: return

                    activity.apply {
                        showFragment(
                            BookmarkDetailFragment.createInstance(
                                target
                            ), null)
                    }
                }

                override fun onItemLongClicked(user: String, star: Star?): Boolean {
                    val items = arrayListOf<Pair<String, ()->Any>>(
                        "ブクマしたエントリを見る" to {
                            (activity as FragmentContainerActivity).apply {
                                showFragment(
                                    UserEntriesFragment.createInstance(
                                        user
                                    ), null)
                            }
                        })

                    if (mStarsTabMode == StarsTabMode.TO_USER && HatenaClient.account?.name == user) {
                        items.add("スターを削除する" to {
                            launch(Dispatchers.Main) {
                                try {
                                    val tasks = ArrayList<Deferred<Any>>()
                                    for (i in 1..star!!.count) {
                                        tasks.add(HatenaClient.deleteStarAsync(mBookmark.getBookmarkUrl(bookmarksEntry), star))
                                    }
                                    tasks.awaitAll()
                                    removeItem(user)
                                    activity.showToast("${user}へのスターを削除しました")
                                }
                                catch (e: Exception) {
                                    activity.showToast("${user}へのスターの削除に失敗しました")
                                    Log.d("failedToDeleteStar", Log.getStackTraceString(e))
                                }
                            }
                        })
                    }

                    AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setTitle(user)
                        .setNegativeButton("Cancel", null)
                        .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                            items[which].second()
                        }
                        .show()

                    return true
                }
            }
            adapter = mStarsAdapter
        }

        // スワイプ更新機能の設定
        view.findViewById<SwipeRefreshLayout>(R.id.stars_swipe_layout).apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(activity.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(activity.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                launch(Dispatchers.Main) {
                    try {
                        mBookmarkDetailFragment?.updateStars()?.join()
                    }
                    catch (e: Exception) {
                        activity.showToast("スターリスト更新失敗")
                        Log.d("FailedToUpdateStars", Log.getStackTraceString(e))
                    }
                    finally {
                        this@swipeLayout.isRefreshing = false
                    }
                }
            }
        }

        retainInstance = true
        return view
    }

    fun scrollToTop() {
        mRoot.findViewById<RecyclerView>(R.id.stars_list).scrollToPosition(0)
    }
}
