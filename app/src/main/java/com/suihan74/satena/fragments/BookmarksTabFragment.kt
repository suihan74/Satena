package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.BookmarksEntry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.StarsEntry
import com.suihan74.utilities.*
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.adapters.BookmarksAdapter
import com.suihan74.satena.adapters.tabs.BookmarksTabAdapter
import com.suihan74.satena.BrowserToolbarManager
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarksTabFragment : CoroutineScopeFragment() {
    private lateinit var mView : View
    private lateinit var mRecyclerView: RecyclerView

    private lateinit var mBookmarksAdapter: BookmarksAdapter
    private lateinit var mParentTabAdapter : BookmarksTabAdapter
    private lateinit var mTabType : BookmarksTabType

    private var mBookmarks : List<Bookmark> = emptyList()
    private lateinit var mBookmarksEntry : BookmarksEntry
    private lateinit var mStarsMap : Map<String, StarsEntry>

    companion object {
        fun createInstance(
            b: List<Bookmark>,
            bookmarksEntry: BookmarksEntry,
            starsMap: Map<String, StarsEntry>,
            adapter: BookmarksTabAdapter,
            type: BookmarksTabType
        ) = BookmarksTabFragment().apply {
            mBookmarks = b
            mBookmarksEntry = bookmarksEntry
            mStarsMap = starsMap
            mParentTabAdapter = adapter
            mTabType = type
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks_tab, container, false)
        mView = view

        // initialize mBookmarks list
        val viewManager = LinearLayoutManager(context)
        mRecyclerView = view.findViewById(R.id.bookmarks_list)
        mBookmarksAdapter = object : BookmarksAdapter(this, ArrayList(mBookmarks), mBookmarksEntry, mStarsMap, mTabType) {
            fun ignoreUser(b: Bookmark) {
                launch(Dispatchers.Main) {
                    try {
                        HatenaClient.ignoreUserAsync(b.user).await()
                        when (mTabType) {
                            BookmarksTabType.POPULAR,
                            BookmarksTabType.RECENT -> removeItem(b)
                            BookmarksTabType.ALL -> {
                                val prefs = SafeSharedPreferences.create<PreferenceKey>(context!!)
                                val showIgnoredUsersInAllTab = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS)
                                if (!showIgnoredUsersInAllTab) {
                                    removeItem(b)
                                }
                            }
                        }
                        activity!!.showToast("id:${b.user}を非表示にしました")
                    }
                    catch (e: Exception) {
                        Log.d("FailedToIgnoreUser", Log.getStackTraceString(e))
                        activity!!.showToast("id:${b.user}を非表示にできませんでした")
                    }

                    try {
                        HatenaClient.getIgnoredUsersAsync().await()
                    }
                    catch (e: Exception) {
                        Log.d("FailedToUpdateIgnores", Log.getStackTraceString(e))
                    }
                }
            }

            fun unignoreUser(b: Bookmark) {
                launch(Dispatchers.Main) {
                    try {
                        HatenaClient.unignoreUserAsync(b.user).await()
                        activity!!.showToast("id:${b.user}の非表示を解除しました")
                    }
                    catch (e: Exception) {
                        Log.d("FailedToIgnoreUser", Log.getStackTraceString(e))
                        activity!!.showToast("id:${b.user}の非表示を解除できませんでした")
                    }

                    try {
                        HatenaClient.getIgnoredUsersAsync().await()
                    }
                    catch (e: Exception) {
                        Log.d("FailedToUpdateIgnores", Log.getStackTraceString(e))
                    }
                }
            }

            fun removeBookmark(b: Bookmark) {
                launch(Dispatchers.Main) {
                    try {
                        (activity as BookmarksActivity).let {
                            val entry = it.entry
                            HatenaClient.deleteBookmarkAsync(entry.url).await()
                            removeItem(b)
                            activity!!.showToast("ブクマを削除しました")
                        }
                    }
                    catch (e: Exception) {
                        Log.d("FailedToIgnoreUser", Log.getStackTraceString(e))
                        activity!!.showToast("ブクマを削除できませんでした")
                    }
                }
            }

            override fun onItemClicked(bookmark: Bookmark) {
                val activity = activity as BookmarksActivity

                val fragment = BookmarkDetailFragment.createInstance(
                    bookmark,
                    mStarsMap,
                    mBookmarksEntry
                )
                activity.showFragment(fragment, null)
            }

            override fun onItemLongClicked(bookmark: Bookmark): Boolean {
                // TODO: ブコメロングタップ時のメニュー表示
                val items = arrayListOf("ブクマ済みエントリ一覧を見る" to {
                    val fragment =
                        UserEntriesFragment.createInstance(bookmark.user)
                    (activity as FragmentContainerActivity).showFragment(fragment, null)
                })
                if (HatenaClient.account?.name == bookmark.user) {
                    items.add("ブックマークを削除" to { removeBookmark(bookmark) })
                }
                else if (HatenaClient.signedIn()) {
                    if (HatenaClient.ignoredUsers.contains(bookmark.user)) {
                        items.add("ユーザー非表示を解除" to { unignoreUser(bookmark) })
                    }
                    else {
                        items.add("ユーザーを非表示" to { ignoreUser(bookmark) })
                    }
                }

                val analyzedBookmarkComment = BookmarkCommentDecorator.convert(bookmark.comment)
                for (url in analyzedBookmarkComment.urls) {
                    items.add(url to {
                        val intent = CustomTabsIntent.Builder(null)
                            .setShowTitle(true)
                            .enableUrlBarHiding()
                            .addDefaultShareMenuItem()
                            .setToolbarColor(ContextCompat.getColor(context!!, R.color.colorPrimary))
                            .setSecondaryToolbarViews(
                                BrowserToolbarManager.createRemoteViews(context!!, null),
                                BrowserToolbarManager.getClickableIds(),
                                BrowserToolbarManager.getOnClickPendingIntent(context!!)
                            )
                            .build()
                        intent.launchUrl(activity, Uri.parse(url))
                    })
                }

                AlertDialog.Builder(context, R.style.AlertDialogStyle)
                    .setTitle("id:${bookmark.user}")
                    .setNegativeButton("Cancel", null)
                    .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                        items[which].second()
                        super.notifyItemChanged(bookmark)
                    }
                    .show()

                return super.onItemLongClicked(bookmark)
            }
        }
        mRecyclerView.apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
                R.drawable.recycler_view_item_divider
            )!!)
            addItemDecoration(dividerItemDecoration)
            layoutManager = viewManager
            adapter = mBookmarksAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    mParentTabAdapter.onScrolled(recyclerView, dx, dy)
                    super.onScrolled(recyclerView, dx, dy)
                }
            })
        }


        // スワイプ更新機能の設定
        view.findViewById<SwipeRefreshLayout>(R.id.bookmarks_swipe_layout).apply {
            val swiper = this
            setProgressBackgroundColorSchemeColor(activity!!.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(activity!!.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                launch(Dispatchers.Main) {
                    try {
                        val bookmarksActivity = activity as BookmarksActivity
                        bookmarksActivity.bookmarksFragment?.refreshBookmarksAsync()?.await()
//                        mRecyclerView.scrollToPosition(0)
                    }
                    catch (e: Exception) {
                        activity!!.showToast("ブックマークリスト更新失敗")
                        Log.d("FailedToUpdateBookmarks", Log.getStackTraceString(e))
                    }
                    finally {
                        swiper.isRefreshing = false
                    }
                }
            }
        }

        retainInstance = true
        return view
    }

    fun setBookmarks(b: List<Bookmark>) {
        mBookmarks = b
        if (this::mBookmarksAdapter.isInitialized) {
            launch(Dispatchers.Main) {
                mBookmarksAdapter.setBookmarks(b)
                scrollToTop()
            }
        }
    }

    fun setSearchText(text : String) {
        if (this::mBookmarksAdapter.isInitialized) {
            mBookmarksAdapter.searchText = text
        }
    }

    fun scrollToTop() {
        mRecyclerView.scrollToPosition(0)
    }

    fun scrollToBottom() {
        if (this::mBookmarksAdapter.isInitialized) {
            if (mBookmarksAdapter.itemCount > 0) {
                mRecyclerView.scrollToPosition(mBookmarksAdapter.itemCount - 1)
            }
        }
    }

    fun scrollTo(user: String) {
        if (this::mBookmarksAdapter.isInitialized) {
            val layoutManager = mRecyclerView.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(mBookmarksAdapter.getItemPosition(user), 0)
        }
    }
}
