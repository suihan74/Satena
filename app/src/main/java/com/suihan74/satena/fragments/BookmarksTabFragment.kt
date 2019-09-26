package com.suihan74.satena.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
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
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.activities.ActivityBase
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.activities.MainActivity
import com.suihan74.satena.adapters.BookmarksAdapter
import com.suihan74.satena.adapters.tabs.BookmarksTabAdapter
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.*
import com.suihan74.satena.showCustomTabsIntent
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksTabFragment : CoroutineScopeFragment() {
    private lateinit var mView : View
    private lateinit var mRecyclerView: RecyclerView

    private lateinit var mBookmarksAdapter: BookmarksAdapter
    private lateinit var mParentTabAdapter: BookmarksTabAdapter
    private var mTabType : BookmarksTabType = BookmarksTabType.RECENT

    private var mBookmarksFragment: BookmarksFragment? = null

    private var mIsIgnoredUsersShownInAll : Boolean = false
    private var mIgnoredWords : List<String> = emptyList()

    val userTagsContainer
        get() = mBookmarksFragment!!.userTagsContainer

    companion object {
        fun createInstance(
            parentTabAdapter: BookmarksTabAdapter,
            type: BookmarksTabType
        ) = BookmarksTabFragment().apply {
            mParentTabAdapter = parentTabAdapter
            mTabType = type
        }

        private const val BUNDLE_TAB_TYPE = "mTabType"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putInt(BUNDLE_TAB_TYPE, mTabType.int)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        mIsIgnoredUsersShownInAll = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS)

        val ignoredEntriesPrefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context)
        val ignoredEntries = ignoredEntriesPrefs.getNullable<List<IgnoredEntry>>(IgnoredEntriesKey.IGNORED_ENTRIES) ?: emptyList()
        mIgnoredWords = ignoredEntries
            .filter { IgnoredEntryType.TEXT == it.type && it.target contains IgnoreTarget.BOOKMARK }
            .map { it.query }

        savedInstanceState?.let {
            mTabType = BookmarksTabType.fromInt(it.getInt(BUNDLE_TAB_TYPE))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks_tab, container, false)
        mView = view

        val activity = activity as? BookmarksActivity ?: throw IllegalStateException("BookmarksTabFragment has created from an invalid activity")
        mBookmarksFragment = activity.bookmarksFragment

        // initialize mBookmarks list
        val viewManager = LinearLayoutManager(context)
        mRecyclerView = view.findViewById(R.id.bookmarks_list)

        mBookmarksAdapter = generateBookmarksAdapter(getBookmarks(mBookmarksFragment!!), mBookmarksFragment!!.bookmarksEntry!!)

        mParentTabAdapter = mBookmarksFragment!!.bookmarksTabAdapter!!
        mRecyclerView.apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
                R.drawable.recycler_view_item_divider
            )!!)
            addItemDecoration(dividerItemDecoration)
            layoutManager = viewManager
            adapter = mBookmarksAdapter

            if (BookmarksTabType.POPULAR != mTabType) {
                val bookmarksUpdater = object : RecyclerViewScrollingUpdater(mBookmarksAdapter) {
                    override fun load() {
                        val fragment = mBookmarksFragment!!
                        val lastOfAll = fragment.bookmarksEntry!!.bookmarks.lastOrNull()
                        val lastOfRecent = fragment.recentBookmarks.lastOrNull()
                        if (lastOfAll?.user == lastOfRecent?.user) {
                            loadCompleted()
                            return
                        }

                        launch(Dispatchers.Main) {
                            mBookmarksAdapter.loadableFooter?.showProgressBar()
                            try {
                                fragment.getNextBookmarksAsync().await()
                                mParentTabAdapter.update()
                            }
                            catch (e: Exception) {
                                Log.d("FailedToFetchEntries", Log.getStackTraceString(e))
                                context?.showToast("ブクマリスト更新失敗")
                            }
                            finally {
                                loadCompleted()
                                mBookmarksAdapter.loadableFooter?.hideProgressBar()
                            }
                        }
                    }
                }
                mRecyclerView.addOnScrollListener(bookmarksUpdater)
            }

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    mParentTabAdapter.onScrolled(recyclerView, dx, dy)
                    super.onScrolled(recyclerView, dx, dy)
                }
            })
        }


        // スワイプ更新機能の設定
        view.findViewById<SwipeRefreshLayout>(R.id.bookmarks_swipe_layout).apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(activity.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(activity.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                launch(Dispatchers.Main) {
                    try {
                        activity.bookmarksFragment?.refreshBookmarksAsync()?.await()
                        scrollToTop()
                    }
                    catch (e: Exception) {
                        activity.showToast("ブックマークリスト更新失敗")
                        Log.d("FailedToUpdateBookmarks", Log.getStackTraceString(e))
                    }
                    finally {
                        this@swipeLayout.isRefreshing = false
                    }
                }
            }
        }

        //retainInstance = true
        return view
    }


    fun getBookmarks(fragment: BookmarksFragment) = when(mTabType) {
        BookmarksTabType.POPULAR ->
            fragment.popularBookmarks.filter {
                !isBookmarkIgnored(it) && !fragment.ignoredUsers.contains(it.user)
            }

        BookmarksTabType.RECENT ->
            fragment.recentBookmarks.filter {
                !it.comment.isBlank() && !isBookmarkIgnored(it) && !fragment.ignoredUsers.contains(it.user)
            }

        BookmarksTabType.ALL -> {
            val bookmarks = fragment.recentBookmarks
            if (mIsIgnoredUsersShownInAll) {
                bookmarks
            }
            else {
                bookmarks.filterNot {
                    fragment.ignoredUsers.contains(it.user)
                }
            }.filterNot { isBookmarkIgnored(it) }
        }
    }

    fun update() {
        if (this::mBookmarksAdapter.isInitialized) {
            mBookmarksAdapter.setBookmarks(getBookmarks(mBookmarksFragment!!))
        }
    }

    fun removeBookmark(bookmark: Bookmark) {
        if (this::mBookmarksAdapter.isInitialized) {
            mBookmarksAdapter.removeItem(bookmark)
        }
    }

    fun notifyItemChanged(bookmark: Bookmark) {
        if (this::mBookmarksAdapter.isInitialized) {
            mBookmarksAdapter.notifyItemChanged(bookmark)
        }
    }

    private fun isBookmarkIgnored(bookmark: Bookmark) =
        mIgnoredWords.any {
            bookmark.user.contains(it) || bookmark.comment.contains(it) || bookmark.getTagsText(",").contains(it)
        }

    fun isBookmarkShown(bookmark: Bookmark) : Boolean {
        if (isBookmarkIgnored(bookmark)) return false

        val fragment = mBookmarksFragment ?: return false
        return when (mTabType) {
            BookmarksTabType.POPULAR ->
                fragment.popularBookmarks.any { it.user == bookmark.user }

            BookmarksTabType.RECENT ->
                !bookmark.comment.isBlank() && !fragment.ignoredUsers.contains(bookmark.user)

            BookmarksTabType.ALL -> {
                val contains = fragment.bookmarksEntry?.bookmarks?.any { it.user == bookmark.user } == true
                contains && (!fragment.ignoredUsers.contains(bookmark.user) || mIsIgnoredUsersShownInAll)
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
        if (!this::mBookmarksAdapter.isInitialized) return
        mBookmarksFragment!!.launch {
            val allBookmarks = mBookmarksFragment?.bookmarksEntry?.bookmarks ?: return@launch
            val lastItem = allBookmarks.lastOrNull { isBookmarkShown(it) }
            if (lastItem != null) {
                scrollAfterLoading(lastItem.user)
            }
        }
    }

    fun scrollTo(user: String) {
        if (!this::mBookmarksAdapter.isInitialized) return
        mBookmarksFragment!!.launch {
            val allBookmarks = mBookmarksFragment?.bookmarksEntry?.bookmarks ?: emptyList()
            val item = allBookmarks.firstOrNull { it.user == user }
            if (item == null || !isBookmarkShown(item)) return@launch

            scrollAfterLoading(user)
        }
    }

    private suspend fun scrollAfterLoading(user: String) {
        val layoutManager = mRecyclerView.layoutManager as LinearLayoutManager
        val position = mBookmarksAdapter.getItemPosition(user)
        if (position >= 0) {
            withContext(Dispatchers.Main) {
                layoutManager.scrollToPositionWithOffset(position, 0)
            }
        }
        else {
            val activity = mBookmarksFragment!!.activity as ActivityBase
            withContext(Dispatchers.Main) {
                activity.showProgressBar(true)
            }
            try {
                while (true) {
                    val diff = mBookmarksFragment!!.getNextBookmarksAsync().await()
                    withContext(Dispatchers.Main) {
                        mParentTabAdapter.update()
                    }
                    if (diff.isEmpty() || diff.any { it.user == user }) {
                        break
                    }
                }
                val pos = mBookmarksAdapter.getItemPosition(user)
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

    private fun generateBookmarksAdapter(bookmarks: List<Bookmark>, bookmarksEntry: BookmarksEntry) : BookmarksAdapter {
        val activity = activity as BookmarksActivity
        return object : BookmarksAdapter(this, bookmarks, bookmarksEntry, mTabType) {
            fun ignoreUser(b: Bookmark) {
                launch(Dispatchers.Main) {
                    try {
                        HatenaClient.ignoreUserAsync(b.user).await()

                        // リストから削除
                        when (mTabType) {
                            BookmarksTabType.POPULAR,
                            BookmarksTabType.RECENT -> removeItem(b)

                            BookmarksTabType.ALL -> if (!mIsIgnoredUsersShownInAll) {
                                removeItem(b)
                            }
                        }

                        activity.showToast("id:${b.user}を非表示にしました")
                    }
                    catch (e: Exception) {
                        Log.d("FailedToIgnoreUser", Log.getStackTraceString(e))
                        activity.showToast("id:${b.user}を非表示にできませんでした")
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
                        activity.showToast("id:${b.user}の非表示を解除しました")
                    }
                    catch (e: Exception) {
                        Log.d("FailedToIgnoreUser", Log.getStackTraceString(e))
                        activity.showToast("id:${b.user}の非表示を解除できませんでした")
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
                        activity.let {
                            val entry = mBookmarksFragment!!.entry
                            HatenaClient.deleteBookmarkAsync(entry.url).await()

                            // すべてのタブのリストから削除する
                            mParentTabAdapter.removeBookmark(b)

                            // キャッシュから削除
                            mBookmarksFragment!!.removeBookmark(b.user)

                            activity.showToast("ブクマを削除しました")
                        }
                    }
                    catch (e: Exception) {
                        Log.d("FailedToIgnoreUser", Log.getStackTraceString(e))
                        activity.showToast("ブクマを削除できませんでした")
                    }
                }
            }

            @SuppressLint("UseSparseArrays")
            fun tagUser(b: Bookmark) {
                val prefs = SafeSharedPreferences.create<UserTagsKey>(context)
                val userTagsContainer = userTagsContainer
                val user = userTagsContainer.addUser(b.user)
                val tags = userTagsContainer.tags
                val tagNames = tags.map { it.name }.toTypedArray()
                val states = tags.map { it.contains(user) }.toBooleanArray()
                val diffs = HashMap<Int, Boolean>()

                AlertDialog.Builder(context, R.style.AlertDialogStyle)
                    .setTitle("ユーザータグを選択")
                    .setMultiChoiceItems(tagNames, states) { _, which, isChecked ->
                        diffs[which] = isChecked
                    }
                    .setNeutralButton("新規タグに登録") { _, _ ->
                        val dialog = UserTagDialogFragment.createInstance { _, name, _ ->
                            if (userTagsContainer.containsTag(name)) {
                                context!!.showToast("既に存在するタグです")
                                return@createInstance false
                            }
                            else {
                                val tag = userTagsContainer.addTag(name)
                                userTagsContainer.tagUser(user, tag)

                                prefs.edit {
                                    putObject(UserTagsKey.CONTAINER, userTagsContainer)
                                }

                                mParentTabAdapter.notifyItemChanged(b)
                                context!!.showToast("タグ: $name を作成して id:${b.user} を追加しました")
                                return@createInstance true
                            }
                        }
                        dialog.show(fragmentManager!!, "dialog")
                    }
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("OK") { _, _ ->
                        if (diffs.isNotEmpty()) {
                            diffs.forEach {
                                val name = tagNames[it.key]
                                val tag = userTagsContainer.getTag(name)!!
                                if (it.value) {
                                    userTagsContainer.tagUser(user, tag)
                                }
                                else {
                                    userTagsContainer.unTagUser(user, tag)
                                }
                            }

                            prefs.edit {
                                putObject(UserTagsKey.CONTAINER, userTagsContainer)
                            }

                            context!!.showToast("id:${b.user} に${user.tags.size}個のタグを設定しました")
                            mParentTabAdapter.notifyItemChanged(b)
                        }
                    }
                    .show()
            }

            override fun onItemClicked(bookmark: Bookmark) {
                val fragment = BookmarkDetailFragment.createInstance(
                    bookmark
                )
                activity.showFragment(fragment, "detail_id:${bookmark.user}")
            }

            override fun onItemLongClicked(bookmark: Bookmark): Boolean {
                val items = arrayListOf("最近のブックマークを見る" to {
                    val intent = Intent(SatenaApplication.instance, MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_DISPLAY_USER, bookmark.user)
                    }
                    startActivity(intent)
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

                items.add("ユーザータグ" to { tagUser(bookmark) })

                val analyzedBookmarkComment = BookmarkCommentDecorator.convert(bookmark.comment)
                for (url in analyzedBookmarkComment.urls) {
                    items.add(url to {
                        context!!.showCustomTabsIntent(url, this@BookmarksTabFragment)
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
    }
}
