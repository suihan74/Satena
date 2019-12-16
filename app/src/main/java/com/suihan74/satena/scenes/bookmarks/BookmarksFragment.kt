package com.suihan74.satena.scenes.bookmarks

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
import android.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.suihan74.HatenaLib.*
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.satena.scenes.bookmarks.detail.BookmarkDetailFragment
import com.suihan74.satena.scenes.bookmarks.information.EntryInformationFragment
import com.suihan74.satena.scenes.bookmarks.tabs.CustomBookmarksTabFragment
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryRepository
import com.suihan74.satena.scenes.preferences.userTag.UserTagRepository
import com.suihan74.utilities.*
import kotlinx.coroutines.*
import org.threeten.bp.LocalDateTime
import java.net.SocketTimeoutException
import java.util.*

class BookmarksFragment :
    CoroutineScopeFragment(),
    BackPressable
{
    private lateinit var mRoot : View
    private lateinit var mDrawer : DrawerLayout
    private lateinit var mTabLayout : TabLayout
    private var mTabPager : ViewPager? = null

    private lateinit var mFABs : Array<FloatingActionButton>
    private lateinit var mBookmarkButton : TextFloatingActionButton
    private lateinit var mBookmarksScrollMenuButton : FloatingActionButton
    private lateinit var mSearchButton : FloatingActionButton
    private lateinit var mSettingsButton : FloatingActionButton

    private var mAreScrollButtonsVisible = false
    private var mIsHidingButtonsByScrollEnabled : Boolean = true

    // ロード完了と同時に詳細画面に遷移する場合の対象ユーザー
    private var mTargetUser : String? = null

    // プリロード中のブクマ・スター
    private var mPreLoadingTasks : BookmarksActivity.PreLoadingTasks? = null
    private var mFetchStarsTasks = WeakHashMap<String, Deferred<Unit>>()

    /** ViewModel */
    lateinit var viewModel: BookmarksViewModel
        private set

    val entry : Entry
        get() = viewModel.entry

    var bookmarksEntry : BookmarksEntry?
        get() = viewModel.bookmarksEntry
        private set(value) {
            viewModel.bookmarksEntry = value
        }

    val bookmarksTabAdapter : BookmarksTabAdapter?
        get() = mTabPager?.adapter as? BookmarksTabAdapter

    val popularBookmarks
        get() = viewModel.bookmarksDigest?.scoredBookmarks?.map { Bookmark.createFrom(it) } ?: emptyList()

    val recentBookmarks
        get() = viewModel.bookmarksRecent

    val starsMap : Map<String, StarsEntry>
        get() = lock(viewModel) { viewModel.starsMap }

    var ignoredUsers : Set<String> = emptySet()
        private set

    val tags: List<TagAndUsers>
        get() = viewModel.userTags.value ?: emptyList()

    val taggedUsers: List<UserAndTags>
        get() = viewModel.taggedUsers.value ?: emptyList()

    val bookmarked : Boolean
        get() {
            val user = HatenaClient.account?.name
            if (user.isNullOrBlank()) { return false }
            return bookmarksEntry?.bookmarks?.any { it.user == user } == true
        }

    fun getFetchStarsTask(user: String) = mFetchStarsTasks[user]

    override val title: String
        get() = (arguments?.getSerializable(ARG_ENTRY) as? Entry)?.title ?: ""

    companion object {
        fun createInstance(entry: Entry, preLoadingTasks: BookmarksActivity.PreLoadingTasks? = null) = BookmarksFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_ENTRY, entry)
            }
            mPreLoadingTasks = preLoadingTasks
        }

        fun createInstance(entry: Entry, targetUser: String? = null, preLoadingTasks: BookmarksActivity.PreLoadingTasks? = null) = BookmarksFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_ENTRY, entry)
                putString(ARG_TARGET_USER, targetUser)
            }
            mPreLoadingTasks = preLoadingTasks
        }

        private const val ARG_ENTRY = "ARG_ENTRY"
        private const val ARG_TARGET_USER = "ARG_TARGET_USER"
    }

    private fun getSubTitle(bookmarks: List<Bookmark>) : String {
        val commentsCount = bookmarks.count { it.comment.isNotBlank() }
        return if (bookmarks.isEmpty()) "0 users"
        else "${bookmarks.size} user${if (bookmarks.size == 1) "" else "s"}  ($commentsCount comment${if (commentsCount == 1) "" else "s"})"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionSet().addTransition(Fade())

        val factory = BookmarksViewModel.Factory(
            IgnoredEntryRepository(SatenaApplication.instance.ignoredEntryDao),
            UserTagRepository(SatenaApplication.instance.userTagDao)
        )
        viewModel = ViewModelProviders.of(this, factory)[BookmarksViewModel::class.java]
        viewModel.entry = arguments!!.getSerializable(ARG_ENTRY) as Entry

        // 設定のロード
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        mIsHidingButtonsByScrollEnabled = prefs.getBoolean(PreferenceKey.BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING)

        viewModel.tabPosition =
            if (viewModel.tabPosition >= 0) viewModel.tabPosition
            else prefs.getInt(PreferenceKey.BOOKMARKS_INITIAL_TAB)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_bookmarks, container, false)
        mRoot = root

        // ツールバーの設定
        root.findViewById<Toolbar>(R.id.bookmarks_toolbar).apply {
            title = entry.title
        }

        // メインコンテンツの設定
        val tabPager = root.findViewById<ViewPager>(R.id.bookmarks_tab_pager)
        mTabPager = tabPager
        mTabLayout = root.findViewById<TabLayout>(R.id.bookmarks_tab_layout).apply {
            setupWithViewPager(tabPager)
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    viewModel.tabPosition = tab!!.position

                    // 以下の事象を防ぐため，タブ切り替え時に必ずFABを表示する
                    // case: スクロールでFABを隠した後，スクロールできないタブに切り替えるとそのタブでFABが表示できなくなる
                    showFabs()
                }

                override fun onTabUnselected(p0: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    val adapter = tabPager.adapter as? BookmarksTabAdapter
                    val fragment = adapter?.findFragment(tab!!.position)
                    fragment?.scrollToTop()
                }
            })
        }

        mBookmarkButton = root.findViewById(R.id.bookmark_button)
        mBookmarksScrollMenuButton = root.findViewById(R.id.bookmarks_scroll_menu_button)
        mSearchButton = root.findViewById(R.id.search_button)
        mSettingsButton = root.findViewById(R.id.custom_settings_button)

        if (HatenaClient.signedIn()) {
            mFABs = arrayOf(
                mBookmarkButton,
                mBookmarksScrollMenuButton,
                mSearchButton,
                mSettingsButton
            )
        }
        else {
            mFABs = arrayOf(
                mBookmarksScrollMenuButton,
                mSearchButton,
                mSettingsButton
            )
            mBookmarkButton.visibility = View.GONE
        }

        // 検索クエリ入力ボックス
        root.findViewById<EditText>(R.id.bookmarks_search_text).apply {
            visibility = View.GONE
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val adapter = tabPager.adapter as? BookmarksTabAdapter
                    adapter?.forEachFragment {
                        it.setSearchText(text.toString())
                    }
                }
            })
        }

        val mScrollButtons = arrayOf(
            root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_top_button).apply {
                setOnClickListener {
                    val adapter = tabPager.adapter as? BookmarksTabAdapter
                    val tab = adapter?.findFragment(tabPager.currentItem)
                    tab?.scrollToTop()
                    hideScrollButtons()
                }
            },
            root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_my_bookmark_button).apply {
                setOnClickListener {
                    val adapter = tabPager.adapter as? BookmarksTabAdapter
                    val tab = adapter?.findFragment(tabPager.currentItem)
                    tab?.scrollTo(HatenaClient.account?.name ?: "")
                    hideScrollButtons()
                }
            },
            root.findViewById<FloatingActionButton>(R.id.bookmarks_scroll_bottom_button).apply {
                setOnClickListener {
                    val adapter = tabPager.adapter as? BookmarksTabAdapter
                    val tab = adapter?.findFragment(tabPager.currentItem)
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

        // ブコメリストの初期化
        launch(Dispatchers.IO) {
            // ViewModelを初期化
            viewModel.init()

            withContext(Dispatchers.Main) {
                if (savedInstanceState == null && bookmarksEntry == null) {
                    showProgressBar()
                    initializeBookmarks(viewModel.tabPosition)
                }
                else {
                    hideProgressBar(withAnimation = false)
                    restoreBookmarks(viewModel.tabPosition)
                }
            }
        }

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
            hideProgressBar(withAnimation = false)
        }

    }

    private val scrollButtons : Array<FloatingActionButton>
        get() {
            val displayMyBookmarkButton = if (mTabPager?.currentItem == BookmarksTabType.POPULAR.ordinal) {
                val user = HatenaClient.account?.name
                if (user.isNullOrBlank()) {
                    false
                }
                else {
                    viewModel.bookmarksDigest?.scoredBookmarks?.any { it.user == user } ?: false
                }
            }
            else {
                bookmarked
            }

            return if (displayMyBookmarkButton) {
                arrayOf(
                    mRoot.findViewById(R.id.bookmarks_scroll_top_button),
                    mRoot.findViewById(R.id.bookmarks_scroll_my_bookmark_button),
                    mRoot.findViewById(R.id.bookmarks_scroll_bottom_button)
                )
            }
            else {
                arrayOf(
                    mRoot.findViewById(R.id.bookmarks_scroll_top_button),
                    mRoot.findViewById(R.id.bookmarks_scroll_bottom_button)
                )
            }
        }

    private val displayedScrollButtons : Array<FloatingActionButton>
        get() =
            listOf<FloatingActionButton>(
                mRoot.findViewById(R.id.bookmarks_scroll_top_button),
                mRoot.findViewById(R.id.bookmarks_scroll_my_bookmark_button),
                mRoot.findViewById(R.id.bookmarks_scroll_bottom_button)
            )
                .filter { it.visibility == View.VISIBLE }
                .toTypedArray()

    private fun showFabs() {
        val buttons = scrollButtons
        mFABs.forEach {
            if (it != mSettingsButton || viewModel.tabPosition == BookmarksTabType.CUSTOM.ordinal) {
                it.show()
            }
            else {
                // 「カスタム」タブ以外では「カスタム」タブの設定ボタンを隠す
                it.hide()
            }
        }
        if (mAreScrollButtonsVisible) {
            buttons.forEach { it.show() }
        }
    }

    private fun hideFabs() {
        val buttons = scrollButtons
        mFABs.forEach { it.hide() }
        if (buttons[0].visibility == View.VISIBLE && mAreScrollButtonsVisible) {
            buttons.forEach { it.hide() }
        }
    }

    // スクロールでFABを表示切替
    private fun onScrolled(dy: Int) {
        if (!mIsHidingButtonsByScrollEnabled) return

        if (dy > 2) {
            hideFabs()
        }
        else if (dy < -2) {
            showFabs()
        }
    }

    private fun showProgressBar() {
        mFABs.forEach {
            it.alpha = 0f
            it.setOnClickListener(null)
        }

        (activity as? ActivityBase)?.showProgressBar()
    }

    private fun hideProgressBar(withAnimation: Boolean = true) {
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

        (activity as? ActivityBase)?.hideProgressBar(withAnimation)
    }

    private fun hideScrollButtons() {
        val size = mBookmarksScrollMenuButton.layoutParams.height.toFloat()
        val dp18 = resources.getDimension(R.dimen.dp_18)

        val buttons = displayedScrollButtons

        buttons.forEachIndexed { i, fab ->
            val dy = (buttons.size - i) * (size + dp18) - size
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

        val buttons = scrollButtons

        buttons.forEachIndexed { i, fab ->
            val dy = (buttons.size - i) * (size + dp18) - size
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
        val searchEditText = mRoot.findViewById<EditText>(R.id.bookmarks_search_text).apply {
            visibility = viewModel.searchModeEnabled.toVisibility()
        }

        mSearchButton.setOnClickListener {
            viewModel.searchModeEnabled = !viewModel.searchModeEnabled
            searchEditText.visibility = viewModel.searchModeEnabled.toVisibility()

            if (searchEditText.visibility == View.GONE) {
                searchEditText.text.clear()
                activity?.hideSoftInputMethod()
            }
            else {
                searchEditText.requestFocus()
                val imm =
                    context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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

        mSettingsButton.apply {
            if (viewModel.tabPosition != BookmarksTabType.CUSTOM.ordinal) {
                hide()
            }

            setOnClickListener {
                val adapter = mTabPager?.adapter as? BookmarksTabAdapter
                val fragment = adapter?.findFragment(BookmarksTabType.CUSTOM.ordinal) as? CustomBookmarksTabFragment
                fragment?.openSettingsDialog(this@BookmarksFragment)
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
                        entry.url
                    )
                val digestBookmarksTask = mPreLoadingTasks?.bookmarksDigestTask
                    ?: HatenaClient.getDigestBookmarksAsync(entry.url)
                val recentBookmarksTask = mPreLoadingTasks?.bookmarksRecentTask
                    ?: HatenaClient.getRecentBookmarksAsync(entry.url)

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
                viewModel.bookmarksDigest = digestBookmarksTask.await()

                val recents = recentBookmarksTask.await()
                viewModel.bookmarksRecent = makeBookmarksRecent(recents.map { Bookmark.createFrom(it) })
            }
            catch (e: Exception) {
                Log.d("failedToFetchBookmarks", e.message)
                if (bookmarksEntry == null) {
                    bookmarksEntry = BookmarksEntry(entry.id, entry.title, entry.count, entry.url, entry.url, entry.imageUrl, emptyList())
                }
            }

            val entryInfoFragment =
                EntryInformationFragment.createInstance(
                    entry,
                    bookmarksEntry
                )
            mDrawer = mRoot.findViewById(R.id.bookmarks_drawer_layout)
            val drawerToggle = object : ActionBarDrawerToggle(activity, mDrawer, activity.toolbar,
                R.string.drawer_open,
                R.string.drawer_close
            ) {
                override fun onDrawerOpened(drawerView: View) {
                    super.onDrawerOpened(drawerView)
                    activity.hideSoftInputMethod()
                }
            }
            mDrawer.addDrawerListener(drawerToggle)
            drawerToggle.isDrawerIndicatorEnabled = false
            drawerToggle.syncState()

            if (!coroutineContext.isActive) {
                return@withContext
            }

            // ページ情報をDrawerに表示
            childFragmentManager.beginTransaction().apply {
                replace(R.id.entry_information_layout, entryInfoFragment)
                commitAllowingStateLoss()
            }

            val bookmarksEntry = bookmarksEntry!!
            toolbar.title = bookmarksEntry.title
            entryInfoFragment.bookmarksEntry = bookmarksEntry
            startUpdateStarsMap(bookmarksEntry.bookmarks)

            mPreLoadingTasks = null

            mTabPager?.apply {
                adapter = object : BookmarksTabAdapter(activity, this) {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = onScrolled(dy)
                }
                addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                    override fun onPageSelected(position: Int) {
                        // タブを切り替えたらスクロールボタンを隠す
                        // タブによって「自分のブコメまでスクロール」の表示切替が必要なため
                        // TODO: 全部隠さないでアニメーションするようにしたい
                        if (mAreScrollButtonsVisible) {
                            hideScrollButtons()
                        }
                    }
                })

                setCurrentItem(initialTabPosition, false)
            }

            // タブを長押しでデフォルトタブの設定を変更する
            mTabLayout.setOnTabLongClickListener { idx ->
                val context = SatenaApplication.instance.applicationContext
                val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
                val key = PreferenceKey.BOOKMARKS_INITIAL_TAB
                if (prefs.getInt(key) != idx) {
                    prefs.edit {
                        put(key, idx)
                    }
                    val tabTitle = context.getString(BookmarksTabType.fromInt(idx).textId)
                    context.showToast("${tabTitle}タブを最初に表示するようにしました")
                }
                return@setOnTabLongClickListener true
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

        val entryInfoFragment =
            EntryInformationFragment.createInstance(
                entry,
                bookmarksEntry
            )
        mDrawer = mRoot.findViewById(R.id.bookmarks_drawer_layout)
        // ページ情報をDrawerに表示
        childFragmentManager.beginTransaction().apply {
            replace(R.id.entry_information_layout, entryInfoFragment)
            commit()
        }

        toolbar.title = bookmarksEntry!!.title
        entryInfoFragment.bookmarksEntry = bookmarksEntry

        mTabPager?.apply {
            adapter = object : BookmarksTabAdapter(activity, this) {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = onScrolled(dy)
            }
            addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
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
        recentBookmarks
            .plus(recentBookmarks)
            .distinctBy { it.user }
            .sortedByDescending { it.timestamp }

    private fun updateRecentBookmarksAsync(updateStarsMap: Boolean = false) = async {
        val recentBookmarks = ArrayList<BookmarkWithStarCount>()
        var of: Long? = null
        try {
            while (true) {
                val response = HatenaClient.getRecentBookmarksAsync(entry.url, of = of).await()

                if (recentBookmarks.isEmpty()) {
                    recentBookmarks.addAll(response)
                    break
                }

                recentBookmarks.addAll(response)

                val existedLatest = recentBookmarks.first()
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

        viewModel.bookmarksRecent = makeBookmarksRecent(bookmarks)
    }

    suspend fun getNextBookmarks() : List<Bookmark> {
        if (recentBookmarks.isEmpty()) return emptyList()
        if (recentBookmarks.last().timestamp <= bookmarksEntry!!.bookmarks.last().timestamp) return emptyList()

        try {
            val of = recentBookmarks.size - 1L
            val response = HatenaClient.getRecentBookmarksAsync(entry.url, of = of).await()

            val newer = response
                .filterNot { recentBookmarks.any { exists -> exists.user == it.user } }
                .map { Bookmark.createFrom(it) }

            viewModel.bookmarksRecent = makeBookmarksRecent(newer)

            return newer
        }
        catch (e: Exception) {
            Log.e("FailedToFetchBookmarks", e.message)
            return emptyList()
        }
    }

    suspend fun updateStar(bookmark: Bookmark) {
        val url = bookmark.getBookmarkUrl(entry)
        try {
            val starsEntry = HatenaClient.getStarsEntryAsync(url).await()
            lock(viewModel) {
                viewModel.starsMap[bookmark.user] = starsEntry
            }
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
                        !starsMap.contains(it.user) &&
                        !mFetchStarsTasks.contains(it.user)
            }

        val urls = list
            .map { it.getBookmarkUrl(entry) }

        if (urls.isEmpty()) return

        val task = async(Dispatchers.IO) {
            for (i in 1..5) {
                try {
                    val entries = HatenaClient.getStarsEntryAsync(urls).await()
                    lock(viewModel) {
                        for (b in bookmarks) {
                            viewModel.starsMap[b.user] =
                                entries.firstOrNull { it.url == b.getBookmarkUrl(entry) }
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
        val getBookmarksEntryTask = HatenaClient.getBookmarksEntryAsync(entry.url)
        val getDigestBookmarksTask = HatenaClient.getDigestBookmarksAsync(entry.url)
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
        viewModel.bookmarksDigest = getDigestBookmarksTask.await()

        if (bookmarksEntry != null) {
//            val activity = activity as ActivityBase
//            activity.toolbar!!.subtitle = getSubtitle(bookmarksEntry?.bookmarks ?: ArrayList())

            view?.findViewById<Toolbar>(R.id.bookmarks_toolbar)?.apply {
                subtitle = getSubTitle(bookmarksEntry?.bookmarks ?: ArrayList())
            }
        }

        val adapter = mTabPager?.adapter as? BookmarksTabAdapter
        adapter?.update()
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
                        user = BookmarkWithStarCount.User(b.user, b.userIconUrl),
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

        viewModel.entry = entry.plusBookmarkedData(result)
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

        if (viewModel.bookmarksDigest != null) {
            val bd = viewModel.bookmarksDigest!!
            viewModel.bookmarksDigest = BookmarksDigest(
                referredBlogEntries = bd.referredBlogEntries,
                scoredBookmarks = plusBookmarkToDigest(bookmark, bd.scoredBookmarks),
                favoriteBookmarks = bd.favoriteBookmarks)
        }

        viewModel.bookmarksRecent = plusBookmarkToList(bookmark, viewModel.bookmarksRecent)
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

        if (viewModel.bookmarksDigest != null) {
            val bd = viewModel.bookmarksDigest!!
            viewModel.bookmarksDigest = BookmarksDigest(
                referredBlogEntries = bd.referredBlogEntries,
                scoredBookmarks = bd.scoredBookmarks.filterNot { it.user == user },
                favoriteBookmarks = bd.favoriteBookmarks.filterNot { it.user == user }
            )
        }

        viewModel.bookmarksRecent = viewModel.bookmarksRecent.filterNot { it.user == user }
    }

    fun updateUI() {
        bookmarksTabAdapter?.update()
    }

    override fun onBackPressed() : Boolean {
        try {
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
                viewModel.searchModeEnabled = false
                return true
            }
        }
        catch (e: Exception) {}

        return false
    }
}
