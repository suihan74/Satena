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
import com.suihan74.satena.R
import com.suihan74.satena.activities.ActivityBase
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.adapters.tabs.BookmarksTabAdapter
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.*
import kotlinx.coroutines.*
import org.threeten.bp.LocalDateTime
import java.net.SocketTimeoutException
import java.util.*

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
    private var mBookmarksRecent : List<Bookmark> = emptyList()

    // ロード完了と同時に詳細画面に遷移する場合の対象ユーザー
    private var mTargetUser : String? = null

    // プリロード中のブクマ・スター
    private var mPreLoadingTasks : BookmarksActivity.PreLoadingTasks? = null
    private var mFetchStarsTasks = WeakHashMap<String, Deferred<Unit>>()

    private var mIsScrollToMyBookmarkButtonEnabled = false

    var bookmarksEntry : BookmarksEntry? = null
        private set

    val bookmarksTabAdapter : BookmarksTabAdapter
        get() = mTabPager.adapter as BookmarksTabAdapter

    val popularBookmarks
        get() = mBookmarksDigest?.scoredBookmarks?.map { Bookmark.createFrom(it) } ?: emptyList()

    val recentBookmarks
        get() = mBookmarksRecent

    val starsMap : Map<String, StarsEntry>
        get() = lock(mStarsMap) { mStarsMap }

    var ignoredUsers : Set<String> = emptySet()
        private set

    fun getFetchStarsTask(user: String) = mFetchStarsTasks[user]

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
                    val fragment = adapter.findFragment(tab!!.position)
                    fragment.scrollToTop()
                }
            })
        }

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
                    val ignoredUsersTask = HatenaClient.getIgnoredUsersAsync()
                    val bookmarksEntryTask = mPreLoadingTasks?.bookmarksTask ?: HatenaClient.getBookmarksEntryAsync(mEntry.url)
                    val digestBookmarksTask = mPreLoadingTasks?.bookmarksDigestTask ?: HatenaClient.getDigestBookmarksAsync(mEntry.url)
                    val recentBookmarksTask = mPreLoadingTasks?.bookmarksRecentTask ?: HatenaClient.getRecentBookmarksAsync(mEntry.url)

                    listOf(
                        ignoredUsersTask,
                        digestBookmarksTask,
                        recentBookmarksTask,
                        bookmarksEntryTask
                    ).awaitAll()

                    ignoredUsers = ignoredUsersTask.await().toSet()
                    bookmarksEntry = bookmarksEntryTask.await()
                    mBookmarksDigest = digestBookmarksTask.await()

                    val recents = recentBookmarksTask.await()
                    mBookmarksRecent = makeBookmarksRecent(recents.map { Bookmark.createFrom(it) })

                    val mBookmarksEntry = bookmarksEntry!!
                    toolbar.title = mBookmarksEntry.title
                    entryInfoFragment.bookmarksEntry = mBookmarksEntry
                    startUpdateStarsMap(mBookmarksEntry.bookmarks)

                    mPreLoadingTasks = null

                    val adapter = object : BookmarksTabAdapter(activity, mTabPager) {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = onScrolled(dy)
                    }
                    mTabPager.apply {
                        this.adapter = adapter
                        addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                            override fun onPageSelected(position: Int) {
                                val tabFragment = adapter.findFragment(position)
                                val user = HatenaClient.account?.name
                                if (user == null) {
                                    changeScrollButtonVisibility(View.GONE)
                                }
                                else {
                                    val bookmarks = when (BookmarksTabType.fromInt(position)) {
                                        BookmarksTabType.POPULAR -> popularBookmarks
                                        else -> bookmarksEntry!!.bookmarks.filter { tabFragment.isBookmarkShown(it) }
                                    }
                                    if (bookmarks.any { it.user == user }) {
                                        changeScrollButtonVisibility(View.VISIBLE)
                                    }
                                    else {
                                        changeScrollButtonVisibility(View.GONE)
                                    }
                                }

                                if (mAreScrollButtonsVisible) {
                                    hideScrollButtons()
                                }
                            }
                        })

                        setCurrentItem(initialTabPosition, false)
                    }

                    /*if (mTargetUser != null) {
                        val tab = adapter.findFragment(initialTabPosition)
                        tab.scrollTo(mTargetUser!!)
                    }*/

                    val scrollToMyBookmarkButton = root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_my_bookmark_button)
                    val userName = HatenaClient.account?.name ?: ""

                    // ブクマ数に比例してUIスレッド停止時間が割と大きくなるので，バックグラウンドで探させて待機する
                    val userBookmarkExists = withContext(Dispatchers.Default) {
                        mBookmarksEntry.bookmarks.none { it.user == userName }
                    }

                    if (!HatenaClient.signedIn() || userBookmarkExists) {
                        mScrollButtons = mScrollButtons.filterNot { it == scrollToMyBookmarkButton }.toTypedArray()
                    }
                }
                catch (e: Exception) {
                    Log.d("FailedToFetchBookmarks", Log.getStackTraceString(e))
                    activity.showToast("ブックマークリスト取得失敗")
                }
                finally {
                    val bookmarks = bookmarksEntry?.bookmarks ?: emptyList()
                    toolbar.subtitle = getSubTitle(bookmarks)

                    if (mTargetUser != null && bookmarksEntry != null) {
                        val bookmark = withContext(Dispatchers.Default) {
                            bookmarksEntry!!.bookmarks.firstOrNull { it.user == mTargetUser }
                        }

                        if (bookmark != null) {
                            val detailFragment =
                                BookmarkDetailFragment.createInstance(
                                    bookmark
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
            val bookmarks = bookmarksEntry?.bookmarks ?: emptyList()

            toolbar.subtitle = getSubTitle(bookmarks)
            mTabPager.adapter = object : BookmarksTabAdapter(activity, mTabPager) {
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
                            tabAdapter.findFragment(i).apply {
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
                    val tab = adapter.findFragment(mTabPager.currentItem)
                    tab.scrollToTop()
                    hideScrollButtons()
                }
            },
            root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_my_bookmark_button).apply {
                setOnClickListener {
                    val adapter = mTabPager.adapter as BookmarksTabAdapter
                    val tab = adapter.findFragment(mTabPager.currentItem)
                    tab.scrollTo(HatenaClient.account?.name ?: "")
                    hideScrollButtons()
                }
            },
            root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_bottom_button).apply {
                setOnClickListener {
                    val adapter = mTabPager.adapter as BookmarksTabAdapter
                    val tab = adapter.findFragment(mTabPager.currentItem)
                    tab.scrollToBottom()
                    hideScrollButtons()
                }
            }
        )

        val size = mBookmarksScrollMenuButton.layoutParams.height.toFloat()
        val dp18 = resources.getDimension(R.dimen.dp_18)
        mScrollButtons.forEachIndexed { i, fab ->
            fab.translationY = (mScrollButtons.size - i) * (size + dp18) - size
            fab.visibility = View.GONE
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

        (activity as? ActivityBase)?.showProgressBar()
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

        (activity as? ActivityBase)?.hideProgressBar()
    }

    private fun hideScrollButtons() {
        val size = mBookmarksScrollMenuButton.layoutParams.height.toFloat()
        val dp18 = resources.getDimension(R.dimen.dp_18)

        mScrollButtons.forEachIndexed { i, fab ->
            val dy = (mScrollButtons.size - i) * (size + dp18) - size
            fab.animate()
                .withEndAction {
                    mAreScrollButtonsVisible = false
                    (fab as View).visibility = View.GONE
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

        val myBookmarksButton = mRoot.findViewById<View>(R.id.bookmarks_scroll_my_bookmark_button)

        val buttons = mScrollButtons.filter {
            it != myBookmarksButton || mIsScrollToMyBookmarkButtonEnabled
        }

        buttons.forEachIndexed { i, fab ->
            val dy = (mScrollButtons.size - i) * (size + dp18) - size
            fab.animate()
                .withStartAction {
                    mAreScrollButtonsVisible = true
                    (fab as View).visibility = View.VISIBLE
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

    private fun makeBookmarksRecent(recents: List<Bookmark>) =
        mBookmarksRecent
            .plus(recents)
            .distinctBy { it.user }
            .sortedByDescending { it.timestamp }

    private fun updateRecentBookmarksAsync(updateStarsMap: Boolean = false) = async {
        val recentBookmarks = ArrayList<BookmarkWithStarCount>()
        var of: Long? = null
        try {
            while (true) {
                val response = HatenaClient.getRecentBookmarksAsync(mEntry.url, of = of).await()

                if (mBookmarksRecent.isEmpty()) {
                    recentBookmarks.addAll(response)
                    break
                }

                recentBookmarks.addAll(response)

                val existedLatest = mBookmarksRecent.first()
                val responseLast = response.lastOrNull()
                if ((responseLast?.timestamp ?: LocalDateTime.MIN) <= existedLatest.timestamp) break
                if (response.isEmpty()) break

                of = response.size - 1L
            }
        }
        catch (e: Exception) {
            Log.e("FailedToFetchBookmarks", e.message)
        }

        val bookmarks = recentBookmarks.map { Bookmark.createFrom(it) }

        if (updateStarsMap) {
            startUpdateStarsMap(bookmarks)
        }

        mBookmarksRecent = makeBookmarksRecent(bookmarks)
    }

    fun getNextBookmarksAsync() : Deferred<List<Bookmark>> = async {
        if (mBookmarksRecent.isEmpty()) return@async emptyList<Bookmark>()

        try {
            val of = mBookmarksRecent.size - 1L
            val response = HatenaClient.getRecentBookmarksAsync(mEntry.url, of = of).await()

            val newer = response
                .filterNot { mBookmarksRecent.any { exists -> exists.user == it.user } }
                .map { Bookmark.createFrom(it) }

            mBookmarksRecent = makeBookmarksRecent(newer)

            return@async newer
        }
        catch (e: Exception) {
            Log.e("FailedToFetchBookmarks", e.message)
            return@async emptyList<Bookmark>()
        }
    }

    suspend fun updateStar(bookmark: Bookmark) {
        val starsEntry = HatenaClient.getStarsEntryAsync(bookmark.getBookmarkUrl(mEntry)).await()
        mStarsMap[bookmark.user] = starsEntry
    }

    private suspend fun startUpdateStarsMap(bookmarks: List<Bookmark>) {
        val list = bookmarks
            .filter {
                it.comment.isNotEmpty() &&
                it.starCount?.isEmpty() != true &&
                !mStarsMap.contains(it.user) &&
                !mFetchStarsTasks.contains(it.user)
            }

        val urls = list
            .map { it.getBookmarkUrl(mEntry) }

        if (urls.isEmpty()) return

        val task = async(Dispatchers.IO) {
            for (i in 1..5) {
                try {
                    val entries = HatenaClient.getStarsEntryAsync(urls).await()
                    lock(mStarsMap) {
                        for (b in bookmarks) {
                            mStarsMap[b.user] =
                                entries.firstOrNull { it.url == b.getBookmarkUrl(mEntry) }
                                    ?: continue
                        }
                    }
                    break
                } catch (e: SocketTimeoutException) {
                    Log.d("Timeout", e.message)
                }
            }
            return@async
        }

        list.forEach {
            mFetchStarsTasks[it.user] = task
        }
    }

    // ブックマーク取得
    fun refreshBookmarksAsync() = async(Dispatchers.Main) {
        val getIgnoredUsersTask = HatenaClient.getIgnoredUsersAsync()
        val getBookmarksEntryTask = HatenaClient.getBookmarksEntryAsync(mEntry.url)
        val getDigestBookmarksTask = HatenaClient.getDigestBookmarksAsync(mEntry.url)
        val updateRecentBookmarksTask = updateRecentBookmarksAsync()

        val tasks = listOf(
            getIgnoredUsersTask,
            getBookmarksEntryTask,
            getDigestBookmarksTask,
            updateRecentBookmarksTask
        )
        tasks.awaitAll()

        ignoredUsers = getIgnoredUsersTask.await().toSet()
        bookmarksEntry = getBookmarksEntryTask.await()
        mBookmarksDigest = getDigestBookmarksTask.await()

        if (bookmarksEntry != null) {
            view?.findViewById<Toolbar>(R.id.bookmarks_toolbar)?.apply {
                subtitle = getSubTitle(bookmarksEntry?.bookmarks ?: ArrayList())
            }
        }

        val adapter = mTabPager.adapter as BookmarksTabAdapter
        adapter.update()
    }

    fun changeScrollButtonVisibility(visibility: Int) {
        /*if (mAreScrollButtonsVisible) {
            mRoot.findViewById<View>(R.id.bookmarks_scroll_my_bookmark_button).apply {
                this.visibility = visibility
                if (View.VISIBLE == visibility) {
                    (this as FloatingActionButton).show()
                }
            }
        }*/

        mIsScrollToMyBookmarkButtonEnabled = when (visibility) {
            View.VISIBLE -> true
            else -> false
        }
    }

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
