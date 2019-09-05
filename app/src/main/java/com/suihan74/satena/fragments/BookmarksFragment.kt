package com.suihan74.satena.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.transition.Fade
import android.transition.TransitionSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.suihan74.HatenaLib.*
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.adapters.tabs.BookmarksTabAdapter
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.R
import com.suihan74.utilities.*
import kotlinx.coroutines.*
import java.net.SocketTimeoutException
import java.util.ArrayList
import java.util.HashMap

class BookmarksFragment : CoroutineScopeFragment(), BackPressable {
    private lateinit var mRoot : View
    private lateinit var mTabPager : ViewPager
    private lateinit var mTabLayout : TabLayout
    private lateinit var mDrawer : DrawerLayout

    private lateinit var mFABs : Array<FloatingActionButton>
    private lateinit var mBookmarkButton : TextFloatingActionButton
    private lateinit var mBookmarksScrollMenuButton : FloatingActionButton
    private lateinit var mSearchButton : FloatingActionButton

    private lateinit var mScrollButtons : Array<FloatingActionButton>
    private var mAreScrollButtonsVisible = false
    private var mIsHidingButtonsByScrollEnabled : Boolean = true

    private var mEntry : Entry = emptyEntry()
    private val mStarsMap = HashMap<String, StarsEntry>()
    private var mBookmarksDigest : BookmarksDigest? = null

    // ロード完了と同時に詳細画面に遷移する場合の対象ユーザー
    private var mTargetUser : String? = null

    // プリロード中のブクマ・スター
    private var mPreLoadingTasks : BookmarksActivity.PreLoadingTasks? = null

    var bookmarksEntry : BookmarksEntry? = null
        private set

    companion object {
        fun createInstance(entry: Entry, preLoadingTasks: BookmarksActivity.PreLoadingTasks? = null) = BookmarksFragment().apply {
            mEntry = entry
            mPreLoadingTasks = preLoadingTasks
            enterTransition = TransitionSet().addTransition(Fade())
        }

        fun createInstance(targetUser: String, entry: Entry, preLoadingTasks: BookmarksActivity.PreLoadingTasks? = null) = BookmarksFragment().apply {
            mEntry = entry
            mTargetUser = targetUser
            mPreLoadingTasks = preLoadingTasks
            enterTransition = TransitionSet().addTransition(Fade())
        }
    }

    private fun getSubTitle(bookmarks: List<Bookmark>) : String {
        val commentsCount = bookmarks.count { it.comment.isNotBlank() }
        return if (bookmarks.isEmpty()) "0 users"
               else "${bookmarks.size} user${if (bookmarks.size == 1) "" else "s"}  ($commentsCount comment${if (commentsCount == 1) "" else "s"})"
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_bookmarks, container, false)
        mRoot = root

        val activity = activity!! as BookmarksActivity

        val prefs = SafeSharedPreferences.create<PreferenceKey>(activity)
        val initialTabPosition = prefs.getInt(PreferenceKey.BOOKMARKS_INITIAL_TAB)
        mIsHidingButtonsByScrollEnabled = prefs.getBoolean(PreferenceKey.BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING)

        // ツールバーの設定
        val toolbar = root.findViewById<Toolbar>(R.id.bookmarks_toolbar).apply {
            title = mEntry.title
        }

        // メインコンテンツの設定
        mTabPager = root.findViewById(R.id.bookmarks_tab_pager)
        mTabLayout = root.findViewById<TabLayout>(R.id.bookmarks_tab_layout).apply {
            setupWithViewPager(mTabPager)
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(p0: TabLayout.Tab?) {}
                override fun onTabUnselected(p0: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    val adapter = mTabPager.adapter as BookmarksTabAdapter
                    val fragment = adapter.findFragment(mTabPager, tab!!.position)
                    fragment.scrollToTop()
                }
            })
        }

        val childFragmentManager = this.childFragmentManager
        val entryInfoFragment = EntryInformationFragment.createInstance(mEntry, bookmarksEntry)
        mDrawer = root.findViewById(R.id.bookmarks_drawer_layout)
        // ページ情報をDrawerに表示
        childFragmentManager.beginTransaction().apply {
            replace(R.id.entry_information_layout, entryInfoFragment)
            commit()
        }

        mBookmarkButton = root.findViewById(R.id.bookmark_button)
        mBookmarksScrollMenuButton = root.findViewById(R.id.bookmarks_scroll_menu_button)
        mSearchButton = root.findViewById(R.id.search_button)

        if (HatenaClient.signedIn()) {
            mFABs = arrayOf(
                mBookmarkButton,
                mBookmarksScrollMenuButton,
                mSearchButton
            )
        }
        else {
            mFABs = arrayOf(
                mBookmarksScrollMenuButton,
                mSearchButton
            )
            mBookmarkButton.visibility = View.GONE
        }

        if (bookmarksEntry == null && savedInstanceState == null) {
            showProgressBar()

            launch(Dispatchers.Main) {
                try {
                    val digestBookmarksTask = mPreLoadingTasks?.bookmarksDigestTask ?: HatenaClient.getDigestBookmarksAsync(mEntry.url)
                    val bookmarksEntryTask = mPreLoadingTasks?.bookmarksTask ?: HatenaClient.getBookmarksEntryAsync(mEntry.url)

                    listOf(
                        digestBookmarksTask,
                        bookmarksEntryTask
                    ).awaitAll()

                    mBookmarksDigest = digestBookmarksTask.await()
                    bookmarksEntry = bookmarksEntryTask.await()

                    val mBookmarksEntry = bookmarksEntry!!
                    toolbar.title = mBookmarksEntry.title
                    entryInfoFragment.bookmarksEntry = mBookmarksEntry
                    refreshStars(mBookmarksEntry.bookmarks)

                    mPreLoadingTasks = null

                    val adapter = object : BookmarksTabAdapter(
                        childFragmentManager,
                        activity,
                        mBookmarksEntry.bookmarks,
                        mBookmarksDigest,
                        mBookmarksEntry,
                        mStarsMap
                    ) {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = onScrolled(dy)
                    }
                    mTabPager.apply {
                        this.adapter = adapter
                        setCurrentItem(initialTabPosition, false)
                    }

                    if (mTargetUser != null) {
                        val tab = adapter.findFragment(mTabPager, initialTabPosition)
                        tab.scrollTo(mTargetUser!!)
                    }

                    val scrollToMyBookmarkButton = root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_my_bookmark_button)
                    val userName = HatenaClient.account?.name ?: ""

                    // ブクマ数に比例してUIスレッド停止時間が割と大きくなるので，バックグラウンドで探させて待機する
                    val userBookmarkExists = withContext(Dispatchers.Default) {
                        mBookmarksEntry.bookmarks.none { it.user == userName }
                    }

                    if (!HatenaClient.signedIn() || userBookmarkExists) {
                        mScrollButtons = mScrollButtons.filterNot { it == scrollToMyBookmarkButton }.toTypedArray()
                        scrollToMyBookmarkButton.visibility = View.GONE
                    }
                }
                catch (e: Exception) {
                    Log.d("FailedToFetchBookmarks", Log.getStackTraceString(e))
                    activity.showToast("ブックマークリスト取得失敗")
                }
                finally {
                    val bookmarks = bookmarksEntry?.bookmarks ?: ArrayList()
                    toolbar.subtitle = getSubTitle(bookmarks)

                    if (mTargetUser != null && bookmarksEntry != null) {
                        val bookmark = withContext(Dispatchers.Default) {
                            bookmarksEntry!!.bookmarks.firstOrNull { it.user == mTargetUser }
                        }

                        if (bookmark != null) {
                            val detailFragment =
                                BookmarkDetailFragment.createInstance(
                                    bookmark,
                                    mStarsMap,
                                    bookmarksEntry!!
                                )
                            activity.showFragment(detailFragment, null)
                        }
                    }

                    mTargetUser = null
                    hideProgressBar()
                }
            }
        }
        else {
            // ブコメ詳細フラグメントからの復帰時
            val bookmarks = bookmarksEntry?.bookmarks ?: ArrayList()

            toolbar.subtitle = getSubTitle(bookmarks)
            mTabPager.adapter = object : BookmarksTabAdapter(
                childFragmentManager,
                activity,
                bookmarks,
                mBookmarksDigest,
                bookmarksEntry!!,
                mStarsMap
            ) {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = onScrolled(dy)
            }
            mTabPager.setCurrentItem(initialTabPosition, false)

            hideProgressBar()
        }

        // 検索クエリ入力ボックス
        root.findViewById<EditText>(R.id.bookmarks_search_text).apply {
            visibility = View.GONE
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val tabAdapter = mTabPager.adapter as BookmarksTabAdapter?
                    if (tabAdapter != null) {
                        for (i in 0 until tabAdapter.count) {
                            tabAdapter.findFragment(mTabPager, i).apply {
                                setSearchText(text.toString())
                            }
                        }
                    }
                }
            })
        }

        mScrollButtons = arrayOf(
            root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_top_button).apply {
                setOnClickListener {
                    val adapter = mTabPager.adapter as BookmarksTabAdapter
                    val tab = adapter.findFragment(mTabPager, mTabPager.currentItem)
                    tab.scrollToTop()
                    hideScrollButtons()
                }
            },
            root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_my_bookmark_button).apply {
                setOnClickListener {
                    val adapter = mTabPager.adapter as BookmarksTabAdapter
                    val tab = adapter.findFragment(mTabPager, mTabPager.currentItem)
                    tab.scrollTo(HatenaClient.account?.name ?: "")
                    hideScrollButtons()
                }
            },
            root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_bottom_button).apply {
                setOnClickListener {
                    val adapter = mTabPager.adapter as BookmarksTabAdapter
                    val tab = adapter.findFragment(mTabPager, mTabPager.currentItem)
                    tab.scrollToBottom()
                    hideScrollButtons()
                }
            }
        )

        val size = mBookmarksScrollMenuButton.layoutParams.height.toFloat()
        val dp18 = resources.getDimension(R.dimen.dp_18)
        mScrollButtons.forEachIndexed { i, fab ->
            fab.translationY = (mScrollButtons.size - i) * (size + dp18) - size
            fab.visibility = View.INVISIBLE
        }
        mAreScrollButtonsVisible = false

        retainInstance = true
        return root
    }

    override fun onResume() {
        super.onResume()

        val prefs = SafeSharedPreferences.create<PreferenceKey>(activity)

        // ツールバーの設定
        mRoot.findViewById<Toolbar>(R.id.bookmarks_toolbar).apply {
            layoutParams = (layoutParams as AppBarLayout.LayoutParams).apply {
                val switchToolbarDisplay = prefs.getBoolean(PreferenceKey.BOOKMARKS_HIDING_TOOLBAR_BY_SCROLLING)
                scrollFlags =
                    if (switchToolbarDisplay)
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    else 0
            }
        }

        if (bookmarksEntry != null) {
            hideProgressBar()
        }

        mIsHidingButtonsByScrollEnabled = prefs.getBoolean(PreferenceKey.BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING)
    }

    // スクロールでFABを表示切替
    private fun onScrolled(dy: Int) {
        if (!mIsHidingButtonsByScrollEnabled) return

        if (dy > 2) {
            mFABs.forEach { it.hide() }
            if (mScrollButtons[0].visibility == View.VISIBLE && mAreScrollButtonsVisible) {
                mScrollButtons.forEach { it.hide() }
            }
        }
        else if (dy < -2) {
            mFABs.forEach { it.show() }
            if (mAreScrollButtonsVisible) {
                mScrollButtons.forEach { it.show() }
            }
        }
    }

    private fun showProgressBar() {
        mFABs.forEach {
            it.alpha = 0f
            it.setOnClickListener(null)
        }

        (activity as BookmarksActivity?)?.showProgressBar()
    }

    private fun hideProgressBar() {
        try {
            mFABs.forEach {
                it.animate()
                    .alpha(1f)
                    .duration = 400
            }
            initializeFabs()
        }
        catch (e: Exception) {
            Log.d("CanceledLoading", e.message)
        }

        (activity as BookmarksActivity?)?.hideProgressBar()
    }

    private fun hideScrollButtons() {
        val size = mBookmarksScrollMenuButton.layoutParams.height.toFloat()
        val dp18 = resources.getDimension(R.dimen.dp_18)

        mScrollButtons.forEachIndexed { i, fab ->
            val dy = (mScrollButtons.size - i) * (size + dp18) - size
            fab.animate()
                .withEndAction {
                    mAreScrollButtonsVisible = false
                    fab.visibility = View.INVISIBLE
                    fab.isClickable = false
                }
                .alphaBy(1f)
                .alpha(0f)
                .translationYBy(0f)
                .translationY(dy)
                .duration = 100
        }
    }

    private fun showScrollButtons() {
        val size = mBookmarksScrollMenuButton.layoutParams.height.toFloat()
        val dp18 = resources.getDimension(R.dimen.dp_18)

        mScrollButtons.forEachIndexed { i, fab ->
            val dy = (mScrollButtons.size - i) * (size + dp18) - size
            fab.animate()
                .withStartAction {
                    mAreScrollButtonsVisible = true
                    fab.visibility = View.VISIBLE
                    fab.isClickable = true
                }
                .alphaBy(0f)
                .alpha(1f)
                .translationYBy(dy)
                .translationY(0f)
                .duration = 100
        }
    }

    @SuppressLint("RestrictedApi")
    private fun initializeFabs() {
        mSearchButton.setOnClickListener {
            val searchEditText = mRoot.findViewById<EditText>(R.id.bookmarks_search_text)
            searchEditText.visibility = if (searchEditText.visibility == View.GONE) View.VISIBLE else View.GONE
            if (searchEditText.visibility == View.GONE) {
                searchEditText.text.clear()
            }
            else {
                searchEditText.requestFocus()
                val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(searchEditText, 0)
            }
        }

        mBookmarksScrollMenuButton.setOnClickListener {
            if (mAreScrollButtonsVisible) {
                hideScrollButtons()
            }
            else {
                showScrollButtons()
            }
        }


        if (HatenaClient.signedIn()) {
            mBookmarkButton.setOnClickListener {
                val activity = activity as BookmarksActivity
                activity.openBookmarkDialog()
            }
        }
        else {
            mBookmarkButton.apply {
                layoutParams = (layoutParams as CoordinatorLayout.LayoutParams).apply {
                    anchorId = View.NO_ID
                }
                visibility = View.GONE
            }
        }
    }

    private suspend fun refreshStars(bookmarks: List<Bookmark>, times: Int = 0) {
        if (times > 5) {
            activity!!.showToast("スター取得失敗")
            return
        }

        try {
            val urls = bookmarks
                .filter { it.comment.isNotBlank() }
                .map { it.getBookmarkUrl(mEntry) }

            val task =
                mPreLoadingTasks?.starsEntriesTask ?: HatenaClient.getStarsEntryAsync(urls)
            val entries = task.await()
            for (b in bookmarks) {
                val starsEntry = entries.find { it.url == b.getBookmarkUrl(mEntry) }
                mStarsMap[b.user] = starsEntry ?: continue
            }
        } catch (e: SocketTimeoutException) {
            Log.d("Timeout", e.message)
            refreshStars(bookmarks, times + 1)
        }
    }

    // ブックマーク取得
    fun refreshBookmarksAsync() : Deferred<List<Bookmark>> = async {
        val tasks = listOf(
            HatenaClient.getIgnoredUsersAsync(),
            HatenaClient.getBookmarksEntryAsync(mEntry.url)
        )
        tasks.awaitAll()
        bookmarksEntry = tasks.last().await() as BookmarksEntry

        launch(Dispatchers.Main) {
            if (bookmarksEntry != null) {
                view?.findViewById<Toolbar>(R.id.bookmarks_toolbar)?.apply {
                    subtitle = getSubTitle(bookmarksEntry?.bookmarks ?: ArrayList())
                }
            }
        }

        val adapter = mTabPager.adapter as BookmarksTabAdapter
        val bookmarks = bookmarksEntry?.bookmarks ?: emptyList()

        for (i in 0 until adapter.count) {
            val fragment = adapter.findFragment(mTabPager, i)
            fragment.setBookmarks(adapter.filterBookmarks(bookmarks, i))
        }

        return@async adapter.filterBookmarks(bookmarks, mTabLayout.selectedTabPosition)
    }

    fun getStarsEntry(b: Bookmark) : StarsEntry? = mStarsMap[b.user]
    fun getStarsMap() : Map<String, StarsEntry> = mStarsMap

    override fun onBackPressed() : Boolean {
        if (mDrawer.isDrawerOpen(Gravity.END)) {
            mDrawer.closeDrawer(Gravity.END)
            return true
        }
        else if (mAreScrollButtonsVisible && mBookmarksScrollMenuButton.isShown) {
            hideScrollButtons()
            return true
        }

        val searchText = mRoot.findViewById<EditText>(R.id.bookmarks_search_text)
        if (searchText.visibility == View.VISIBLE) {
            searchText.visibility = View.GONE
            return true
        }

        return false
    }
}
