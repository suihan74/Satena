package com.suihan74.satena.scenes.bookmarks

import android.annotation.SuppressLint
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
import com.suihan74.HatenaLib.BookmarksEntry
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.BookmarkDialog
import com.suihan74.satena.dialogs.ReportDialogFragment
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.bookmarks.detail.BookmarkDetailFragment
import com.suihan74.satena.showCustomTabsIntent
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BookmarksTabFragment : CoroutineScopeFragment() {
    protected abstract fun getBookmarks(fragment: BookmarksFragment) : List<Bookmark>
    protected abstract fun isBookmarkShown(bookmark: Bookmark, fragment: BookmarksFragment) : Boolean
    protected abstract fun hideIgnoredBookmark(adapter: BookmarksAdapter, bookmark: Bookmark)

    private lateinit var mView : View
    private lateinit var mRecyclerView: RecyclerView

    private var mBookmarksAdapter: BookmarksAdapter? = null
    private var mTabType : BookmarksTabType = BookmarksTabType.RECENT

    private var mIgnoredWords : List<String> = emptyList()

    private var mBookmarksUpdater : RecyclerViewScrollingUpdater? = null

    val bookmarksFragment
        get() = (activity as? BookmarksActivity)?.bookmarksFragment

    val userTagsContainer
        get() = bookmarksFragment!!.userTagsContainer

    companion object {
        const val ARGS_KEY_TAB_TYPE = "mTabType"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ignoredEntriesPrefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context)
        val ignoredEntries = ignoredEntriesPrefs.getNullable<List<IgnoredEntry>>(IgnoredEntriesKey.IGNORED_ENTRIES) ?: emptyList()
        mIgnoredWords = ignoredEntries
            .filter { IgnoredEntryType.TEXT == it.type && it.target contains IgnoreTarget.BOOKMARK }
            .map { it.query }

        arguments!!.let {
            mTabType = BookmarksTabType.fromInt(it.getInt(ARGS_KEY_TAB_TYPE))
        }
    }

    protected open val isScrollingUpdaterEnabled = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks_tab, container, false)
        mView = view

        val activity = activity as? BookmarksActivity ?: throw IllegalStateException("BookmarksTabFragment has created from an invalid activity")

        // initialize mBookmarks list
        refreshBookmarksAdapter()

        // スクロールで追加分をロード
        val bookmarksUpdater = object : RecyclerViewScrollingUpdater(mBookmarksAdapter!!) {
            override fun load() {
                if (!isScrollingUpdaterEnabled) {
                    loadCompleted()
                    return
                }
                val fragment = bookmarksFragment!!
                val lastOfAll = fragment.bookmarksEntry!!.bookmarks.lastOrNull()
                val lastOfRecent = fragment.recentBookmarks.lastOrNull()
                if (lastOfAll?.user == lastOfRecent?.user) {
                    loadCompleted()
                    return
                }

                launch(Dispatchers.Main) {
                    mBookmarksAdapter?.loadableFooter?.showProgressBar()
                    try {
                        val mustToLoad = withContext(Dispatchers.Default) {
                            fragment.bookmarksEntry?.bookmarks?.any { isBookmarkShown(it) } ?: false
                        }
                        if (mustToLoad) {
                            while (true) {
                                val diff = fragment.getNextBookmarks()
                                if (diff.isEmpty() || diff.any { isBookmarkShown(it) }) break
                            }

                            val parentTabAdapter = bookmarksFragment?.bookmarksTabAdapter
                            parentTabAdapter?.update()
                        }
                    }
                    catch (e: Exception) {
                        Log.d("FailedToFetchEntries", Log.getStackTraceString(e))
                        context?.showToast(getString(R.string.msg_update_bookmarks_failed))
                    }
                    finally {
                        loadCompleted()
                        mBookmarksAdapter?.loadableFooter?.hideProgressBar()
                    }
                }
            }
        }
        mBookmarksUpdater = bookmarksUpdater

        val viewManager = LinearLayoutManager(context)
        mRecyclerView = view.findViewById<RecyclerView>(R.id.bookmarks_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
                R.drawable.recycler_view_item_divider
            )!!)
            addItemDecoration(dividerItemDecoration)
            layoutManager = viewManager
            adapter = mBookmarksAdapter

            addOnScrollListener(bookmarksUpdater)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val parentTabAdapter = bookmarksFragment?.bookmarksTabAdapter
                    parentTabAdapter?.onScrolled(recyclerView, dx, dy)
                    super.onScrolled(recyclerView, dx, dy)
                }
            })
        }


        // スワイプ更新機能の設定
        view.findViewById<SwipeRefreshLayout>(R.id.bookmarks_swipe_layout).apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(activity.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(activity.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                if (bookmarksUpdater.isLoading) {
                    isRefreshing = false
                    return@setOnRefreshListener
                }

                launch(Dispatchers.Main) {
                    try {
                        activity.bookmarksFragment?.refreshBookmarksAsync()?.await()
                        scrollToTop()

                        if (mBookmarksAdapter?.bookmarksCount == 0) {
                            bookmarksUpdater.invokeLoading()
                        }
                    }
                    catch (e: Exception) {
                        activity.showToast(getString(R.string.msg_update_bookmarks_failed))
                        Log.d("FailedToUpdateBookmarks", Log.getStackTraceString(e))
                    }
                    finally {
                        this@swipeLayout.isRefreshing = false
                    }
                }
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        if (mBookmarksAdapter?.bookmarksCount == 0) {
            mBookmarksUpdater?.invokeLoading()
        }
    }


    fun update() {
        mBookmarksAdapter?.setBookmarks(getBookmarks(bookmarksFragment!!))
    }

    fun removeBookmark(bookmark: Bookmark) {
        mBookmarksAdapter?.removeItem(bookmark)
    }

    fun notifyItemChanged(bookmark: Bookmark) {
        mBookmarksAdapter?.notifyItemChanged(bookmark)
    }

    protected fun isBookmarkIgnored(bookmark: Bookmark) =
        mIgnoredWords.any {
            bookmark.user.contains(it) || bookmark.comment.contains(it) || bookmark.getTagsText(",").contains(it)
        }

    private fun isBookmarkShown(bookmark: Bookmark) : Boolean {
        if (isBookmarkIgnored(bookmark)) return false

        val fragment = bookmarksFragment ?: return false
        return isBookmarkShown(bookmark, fragment)
    }

    fun setSearchText(text : String) {
        mBookmarksAdapter?.searchText = text
    }

    fun scrollToTop() {
        mRecyclerView.scrollToPosition(0)
    }

    fun scrollToBottom() {
        if (mBookmarksAdapter == null) return
        bookmarksFragment!!.launch {
            val allBookmarks = bookmarksFragment?.bookmarksEntry?.bookmarks ?: return@launch
            val lastItem = allBookmarks.lastOrNull { isBookmarkShown(it) }
            if (lastItem != null) {
                scrollAfterLoading(lastItem.user)
            }
        }
    }

    fun scrollTo(user: String) {
        if (mBookmarksAdapter == null) return
        bookmarksFragment!!.launch {
            val allBookmarks = bookmarksFragment?.bookmarksEntry?.bookmarks ?: emptyList()
            val item = allBookmarks.firstOrNull { it.user == user }
            if (item == null || !isBookmarkShown(item)) return@launch

            scrollAfterLoading(user)
        }
    }

    suspend fun loadUntil(user: String) : Int {
        val parentTabAdapter = bookmarksFragment?.bookmarksTabAdapter
        val bookmarksAdapter = mBookmarksAdapter ?: throw RuntimeException("views are not initialized")

        val position = bookmarksAdapter.getItemPosition(user)
        if (position >= 0) {
            return position
        }

        while (true) {
            val diff = bookmarksFragment!!.getNextBookmarks()
            withContext(Dispatchers.Main) {
                parentTabAdapter?.update()
            }
            if (diff.isEmpty() || diff.any { it.user == user }) {
                break
            }
        }

        return bookmarksAdapter.getItemPosition(user)
    }

    private suspend fun scrollAfterLoading(user: String) {
        val layoutManager = mRecyclerView.layoutManager as LinearLayoutManager
        val bookmarksAdapter = mBookmarksAdapter ?: return

        val position = bookmarksAdapter.getItemPosition(user)
        if (position >= 0) {
            withContext(Dispatchers.Main) {
                layoutManager.scrollToPositionWithOffset(position, 0)
            }
        }
        else {
            val activity = bookmarksFragment!!.activity as ActivityBase
            withContext(Dispatchers.Main) {
                activity.showProgressBar(true)
            }
            try {
                val pos = loadUntil(user)
                if (pos >= 0) {
                    withContext(Dispatchers.Main) {
                        layoutManager.scrollToPositionWithOffset(pos, 0)
                    }
                }
            }
            catch (e: Exception) {
                Log.e("FailedToScroll", e.message)
            }
            finally {
                withContext(Dispatchers.Main) {
                    activity.hideProgressBar()
                }
            }
        }
    }

    protected fun refreshBookmarksAdapter() {
        mBookmarksAdapter = generateBookmarksAdapter(getBookmarks(bookmarksFragment!!), bookmarksFragment!!.bookmarksEntry!!)
        if (this::mRecyclerView.isInitialized) {
            mRecyclerView.adapter = mBookmarksAdapter
        }
    }

    private fun generateBookmarksAdapter(bookmarks: List<Bookmark>, bookmarksEntry: BookmarksEntry) : BookmarksAdapter {
        val activity = activity as BookmarksActivity
        return object : BookmarksAdapter(this, bookmarks, bookmarksEntry, mTabType) {
            override fun onItemClicked(bookmark: Bookmark) {
                val fragment =
                    BookmarkDetailFragment.createInstance(
                        bookmark
                    )
                activity.showFragment(fragment, "detail_id:${bookmark.user}")
            }

            override fun onItemLongClicked(bookmark: Bookmark): Boolean {
                val adapter = this
                val dialog = BookmarkDialog.Builder(
                        bookmarksFragment!!,
                        bookmark,
                        bookmarksFragment!!.entry
                    )
                    .setOnRemoveBookmark { b ->
                        // すべてのタブのリストから削除する
                        val parentTabAdapter = bookmarksFragment?.bookmarksTabAdapter
                        parentTabAdapter?.removeBookmark(b)

                        // キャッシュから削除
                        GlobalScope.launch(Dispatchers.Default) {
                            bookmarksFragment?.removeBookmark(b.user)
                        }
                    }
                    .setOnChangeUserIgnoreState { b, isIgnored ->
                        if (isIgnored) {
                            // リストから削除
                            hideIgnoredBookmark(adapter, b)
                        }
                    }
                    .setOnSelectMenuItem { b, _ ->
                        notifyItemChanged(b)
                    }
                    .setOnSelectUrl { url ->
                        context?.showCustomTabsIntent(url, activity)
                    }
                    .setOnTagUser { b ->
                        val parentTabAdapter = bookmarksFragment?.bookmarksTabAdapter
                        parentTabAdapter?.notifyItemChanged(b)
                    }
                    .build()

                dialog.show(childFragmentManager, "dialog")

                return super.onItemLongClicked(bookmark)
            }
        }
    }
}
