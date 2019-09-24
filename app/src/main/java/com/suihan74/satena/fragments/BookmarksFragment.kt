package com.suihan74.satena.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.Fade
import android.transition.TransitionSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.suihan74.HatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.activities.ActivityBase
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.adapters.tabs.BookmarksTabAdapter
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.UserTagsContainer
import com.suihan74.satena.models.UserTagsKey
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

    private var mEntry : Entry = Entry.createEmpty()
    private val mStarsMap = HashMap<String, StarsEntry>()
    private var mBookmarksDigest : BookmarksDigest? = null
    private var mBookmarksRecent : List<Bookmark> = emptyList()

    private lateinit var mUserTagsContainer : UserTagsContainer

    // ロード完了と同時に詳細画面に遷移する場合の対象ユーザー
    private var mTargetUser : String? = null

    // プリロード中のブクマ・スター
    private var mPreLoadingTasks : BookmarksActivity.PreLoadingTasks? = null
    private var mFetchStarsTasks = WeakHashMap<String, Deferred<Unit>>()

    private var mIsScrollToMyBookmarkButtonEnabled = false

    val entry : Entry
        get() = mEntry

    var bookmarksEntry : BookmarksEntry? = null
        private set

    val bookmarksTabAdapter : BookmarksTabAdapter?
        get() = mTabPager.adapter as? BookmarksTabAdapter

    val popularBookmarks
        get() = mBookmarksDigest?.scoredBookmarks?.map { Bookmark.createFrom(it) } ?: emptyList()

    val recentBookmarks
        get() = mBookmarksRecent

    val starsMap : Map<String, StarsEntry>
        get() = lock(mStarsMap) { mStarsMap }

    var ignoredUsers : Set<String> = emptySet()
        private set

    val userTagsContainer
        get() = mUserTagsContainer

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

        private const val BUNDLE_TAB_POSITION = "tabPosition"
        private var savedEntry : Entry? = null
        private var savedBookmarksEntry : BookmarksEntry? = null
        private var savedBookmarksRecent : List<Bookmark>? = null
        private var savedBookmarksDigest : BookmarksDigest? = null
        private var savedStarsMap : HashMap<String, StarsEntry>? = null
    }

    private fun getSubTitle(bookmarks: List<Bookmark>) : String {
        val commentsCount = bookmarks.count { it.comment.isNotBlank() }
        return if (bookmarks.isEmpty()) "0 users"
               else "${bookmarks.size} user${if (bookmarks.size == 1) "" else "s"}  ($commentsCount comment${if (commentsCount == 1) "" else "s"})"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putInt(BUNDLE_TAB_POSITION, mTabPager.currentItem)
        }
        savedEntry = mEntry
        savedBookmarksEntry = bookmarksEntry
        savedBookmarksRecent = mBookmarksRecent
        savedBookmarksDigest = mBookmarksDigest
        savedStarsMap = mStarsMap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.run {
            mEntry = savedEntry!!
            bookmarksEntry = savedBookmarksEntry
            mBookmarksRecent = savedBookmarksRecent!!
            mBookmarksDigest = savedBookmarksDigest
            mStarsMap.putAll(savedStarsMap!!)

            savedEntry = null
            savedBookmarksEntry = null
            savedBookmarksRecent = null
            savedBookmarksDigest = null
            savedStarsMap = null
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_bookmarks, container, false)
        mRoot = root

        val activity = activity!! as BookmarksActivity

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        mIsHidingButtonsByScrollEnabled = prefs.getBoolean(PreferenceKey.BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING)
        val initialTabPosition = savedInstanceState?.run {
            getInt(BUNDLE_TAB_POSITION)
        } ?: prefs.getInt(PreferenceKey.BOOKMARKS_INITIAL_TAB)

        // ユーザータグをロード
        val userTagsPrefs = SafeSharedPreferences.create<UserTagsKey>(context)
        mUserTagsContainer = userTagsPrefs.get(UserTagsKey.CONTAINER)

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
                    fragment?.scrollToTop()
                }
            })
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

        if (savedInstanceState == null) {
            showProgressBar()
            launch(Dispatchers.Main) {
                initializeBookmarks(initialTabPosition)
            }
        }
        else {
            restoreBookmarks(initialTabPosition)
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
                    tabAdapter?.forEachFragment {
                        it.setSearchText(text.toString())
                    }
                }
            })
        }

        mScrollButtons = arrayOf(
            root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_top_button).apply {
                setOnClickListener {
                    val adapter = mTabPager.adapter as BookmarksTabAdapter
                    val tab = adapter.findFragment(mTabPager.currentItem)
                    tab?.scrollToTop()
                    hideScrollButtons()
                }
            },
            root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_my_bookmark_button).apply {
                setOnClickListener {
                    val adapter = mTabPager.adapter as BookmarksTabAdapter
                    val tab = adapter.findFragment(mTabPager.currentItem)
                    tab?.scrollTo(HatenaClient.account?.name ?: "")
                    hideScrollButtons()
                }
            },
            root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_bottom_button).apply {
                setOnClickListener {
                    val adapter = mTabPager.adapter as BookmarksTabAdapter
                    val tab = adapter.findFragment(mTabPager.currentItem)
                    tab?.scrollToBottom()
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
            initializeFABs()
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
    private fun initializeFABs() {
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

    private suspend fun initializeBookmarks(initialTabPosition: Int) = withContext(Dispatchers.Main) {
        val activity = activity!! as BookmarksActivity
        val toolbar = mRoot.findViewById<Toolbar>(R.id.bookmarks_toolbar)

        try {
            try {
                val ignoredUsersTask = HatenaClient.getIgnoredUsersAsync()
                val bookmarksEntryTask =
                    mPreLoadingTasks?.bookmarksTask ?: HatenaClient.getBookmarksEntryAsync(
                        mEntry.url
                    )
                val digestBookmarksTask = mPreLoadingTasks?.bookmarksDigestTask
                    ?: HatenaClient.getDigestBookmarksAsync(mEntry.url)
                val recentBookmarksTask = mPreLoadingTasks?.bookmarksRecentTask
                    ?: HatenaClient.getRecentBookmarksAsync(mEntry.url)

                // recent bookmarksの取得をここまでロードした分までで中止する
                BookmarksActivity.stopPreLoading()

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
            }
            catch (e: Exception) {
                Log.d("failedToFetchBookmarks", e.message)
                if (bookmarksEntry == null) {
                    bookmarksEntry = BookmarksEntry(mEntry.id, mEntry.title, mEntry.count, mEntry.url, mEntry.url, mEntry.imageUrl, emptyList())
                }
            }

            val entryInfoFragment = EntryInformationFragment.createInstance(mEntry, bookmarksEntry)
            mDrawer = mRoot.findViewById(R.id.bookmarks_drawer_layout)

            if (!coroutineContext.isActive) {
                return@withContext
            }

            // ページ情報をDrawerに表示
            childFragmentManager.beginTransaction().apply {
                replace(R.id.entry_information_layout, entryInfoFragment)
                commit()
            }

            val bookmarksEntry = bookmarksEntry!!
            toolbar.title = bookmarksEntry.title
            entryInfoFragment.bookmarksEntry = bookmarksEntry
            startUpdateStarsMap(bookmarksEntry.bookmarks)

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
                                else -> bookmarksEntry.bookmarks.filter { tabFragment?.isBookmarkShown(it) ?: false }
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

            val scrollToMyBookmarkButton = mRoot.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_my_bookmark_button)
            val userName = HatenaClient.account?.name ?: ""

            // ブクマ数に比例してUIスレッド停止時間が割と大きくなるので，バックグラウンドで探させて待機する
            val userBookmarkExists = withContext(Dispatchers.Default) {
                bookmarksEntry.bookmarks.none { it.user == userName }
            }

            if (!HatenaClient.signedIn() || userBookmarkExists) {
                mScrollButtons = mScrollButtons.filterNot { it == scrollToMyBookmarkButton }.toTypedArray()
            }
        }
        catch (e: IllegalStateException) {
            Log.d("Cancelled", Log.getStackTraceString(e))
            return@withContext
        }
        catch (e: Exception) {
            Log.d("FailedToFetchBookmarks", e.message)
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
                    activity.showFragment(detailFragment, "detail_id:${bookmark.user}")
                }
            }

            mTargetUser = null
            hideProgressBar()
        }
    }

    private fun restoreBookmarks(initialTabPosition: Int) {
        val activity = activity!! as BookmarksActivity
        val toolbar = mRoot.findViewById<Toolbar>(R.id.bookmarks_toolbar)

        ignoredUsers = HatenaClient.ignoredUsers.toSet()

        val entryInfoFragment = EntryInformationFragment.createInstance(mEntry, bookmarksEntry)
        mDrawer = mRoot.findViewById(R.id.bookmarks_drawer_layout)
        // ページ情報をDrawerに表示
        childFragmentManager.beginTransaction().apply {
            replace(R.id.entry_information_layout, entryInfoFragment)
            commit()
        }

        toolbar.title = bookmarksEntry!!.title
        entryInfoFragment.bookmarksEntry = bookmarksEntry

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
                            else -> bookmarksEntry!!.bookmarks.filter { tabFragment?.isBookmarkShown(it) ?: false }
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

        val bookmarks = bookmarksEntry?.bookmarks ?: emptyList()
        toolbar.subtitle = getSubTitle(bookmarks)
    }

    private fun makeBookmarksRecent(recentBookmarks: List<Bookmark>) =
        mBookmarksRecent
            .plus(recentBookmarks)
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
        val url = bookmark.getBookmarkUrl(mEntry)
        try {
            val starsEntry =
                HatenaClient.getStarsEntryAsync(url).await()
            mStarsMap[bookmark.user] = starsEntry
        }
        catch (e: Exception) {
            Log.d("updateStar", "stars are empty: $url")
        }
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

    private fun plusBookmarkToList(bookmark: Bookmark, list: List<Bookmark>) : List<Bookmark> {
        val position = list.indexOfFirst { it.user == bookmark.user }

        return if (position < 0) {
            list.plus(bookmark).sortedByDescending { it.timestamp }
        }
        else {
            list.mapIndexed { index, b ->
                if (index == position) {
                    Bookmark(
                        user = b.user,
                        comment = bookmark.comment,
                        tags = bookmark.tags,
                        timestamp = b.timestamp,
                        starCount = b.starCount)
                }
                else b
            }
        }
    }

    private fun plusBookmarkToDigest(bookmark: Bookmark, list: List<BookmarkWithStarCount>) : List<BookmarkWithStarCount> {
        val position = list.indexOfFirst { it.user == bookmark.user }

        return if (position >= 0) {
            list.mapIndexed { index, b ->
                if (index == position) {
                    BookmarkWithStarCount(
                        mUser = User(b.user, b.userIconUrl),
                        comment = bookmark.comment,
                        isPrivate = b.isPrivate,
                        link = b.link,
                        tags = bookmark.tags,
                        timestamp = b.timestamp,
                        starCount = b.starCount)
                }
                else b
            }
        }
        else list
    }

    suspend fun addBookmark(result: BookmarkResult) {
        val bookmark = Bookmark(
            user = result.user,
            comment = result.comment,
            tags = result.tags,
            timestamp = result.timestamp,
            starCount = emptyList()
        )

        mEntry = entry.plusBookmarkedData(result)
        addBookmark(bookmark)
    }

    suspend fun addBookmark(bookmark: Bookmark) = withContext(Dispatchers.Default) {
        if (bookmarksEntry != null) {
            val be = bookmarksEntry!!
            bookmarksEntry = BookmarksEntry(
                id = be.id,
                title = be.title,
                count = be.count,
                url = be.url,
                entryUrl = be.entryUrl,
                screenshot = be.screenshot,
                bookmarks = plusBookmarkToList(bookmark, be.bookmarks))
        }

        if (mBookmarksDigest != null) {
            val bd = mBookmarksDigest!!
            mBookmarksDigest = BookmarksDigest(
                referredBlogEntries = bd.referredBlogEntries,
                scoredBookmarks = plusBookmarkToDigest(bookmark, bd.scoredBookmarks),
                favoriteBookmarks = bd.favoriteBookmarks)
        }

        mBookmarksRecent = plusBookmarkToList(bookmark, mBookmarksRecent)
    }

    suspend fun removeBookmark(user: String) = withContext(Dispatchers.Default) {
        if (bookmarksEntry != null) {
            val be = bookmarksEntry!!
            bookmarksEntry = BookmarksEntry(
                id = be.id,
                title = be.title,
                count = be.count,
                url = be.url,
                entryUrl = be.entryUrl,
                screenshot = be.screenshot,
                bookmarks = be.bookmarks.filterNot { it.user == user })
        }

        if (mBookmarksDigest != null) {
            val bd = mBookmarksDigest!!
            mBookmarksDigest = BookmarksDigest(
                referredBlogEntries = bd.referredBlogEntries,
                scoredBookmarks = bd.scoredBookmarks.filterNot { it.user == user },
                favoriteBookmarks = bd.favoriteBookmarks.filterNot { it.user == user }
            )
        }

        mBookmarksRecent = mBookmarksRecent.filterNot { it.user == user }
    }

    fun updateUI() {
        bookmarksTabAdapter?.update()
    }

    override fun onBackPressed() : Boolean {
        if (mDrawer.isDrawerOpen(GravityCompat.END)) {
            mDrawer.closeDrawer(GravityCompat.END)
            return true
        }
        else if (mAreScrollButtonsVisible && mBookmarksScrollMenuButton.isShown) {
            hideScrollButtons()
            return true
        }

        val searchText = mRoot.findViewById<EditText>(R.id.bookmarks_search_text)
        if (searchText.visibility == View.VISIBLE) {
            searchText.setText("")
            searchText.visibility = View.GONE
            return true
        }

        return false
    }
}
