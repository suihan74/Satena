package com.suihan74.satena.scenes.entries2

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.NetworkReceiver
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityEntries2Binding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.ReleaseNotesDialogFragment
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.ExtraScrollingAlignment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.Theme
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity
import com.suihan74.satena.scenes.bookmarks.BookmarksActivityContract
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.bottomBar.UserBottomItemsSetter
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.views.CustomBottomAppBar
import com.suihan74.utilities.views.bindMenuItemsGravity

class EntriesActivity : AppCompatActivity(), ScrollableToTop {
    companion object {
        /** String: アクティビティ生成時にCategory.Siteに遷移、表示するURL */
        const val EXTRA_SITE_URL = "EntriesActivity.EXTRA_SITE_URL"

        /** String: アクティビティ生成時にCategory.Userに遷移、表示するユーザー */
        const val EXTRA_USER = "EntriesActivity.EXTRA_USER"

        /** String: アクティビティ生成時にCategory.Searchに遷移、タグ検索を行う */
        const val EXTRA_SEARCH_TAG = "EntriesActivity.EXTRA_SEARCH_TAG"

        /** Boolean: アクティビティ生成時にCategory.Noticesに遷移 */
        const val EXTRA_OPEN_NOTICES = "EntriesActivity.EXTRA_OPEN_NOTICES"

        /** リリースノートダイアログ */
        private const val DIALOG_RELEASE_NOTES = "DIALOG_RELEASE_NOTES"

        /** 終了確認ダイアログ */
        private const val DIALOG_TERMINATION = "DIALOG_TERMINATION"
    }

    private val REQUEST_CODE_UPDATE by lazy { hashCode() and 0x0000ffff }

    // ------ //

    /** Entry画面全体で使用するViewModel */
    val viewModel by lazyProvideViewModel {
        val app = SatenaApplication.instance
        val repository = EntriesRepository(
            context = app,
            client = HatenaClient,
            app.accountLoader,
            app.ignoredEntriesRepository,
            app.favoriteSitesRepository,
            app.readEntriesRepository
        )
        EntriesViewModel(repository)
    }

    // ------ //

    private lateinit var binding: ActivityEntries2Binding

    // ------ //

    /** ドロワーの開閉状態 */
    private val isDrawerOpened : Boolean
        get() = binding.drawerLayout.isDrawerOpen(binding.drawerArea)

    /** FABメニューの開閉状態 */
    private var isFABMenuOpened : Boolean = false

    /** エクストラスクロール状態 */
    private val extraScrolling : Boolean
        get() = binding.motionLayout.progress > 0

    /**
     * ボトムバーのSearchView
     *
     * ボトムバー使用モードではない場合nullが返る
     */
    val bottomSearchView : SearchView?
        get() =
            if (viewModel.isBottomLayoutMode) binding.bottomSearchView
            else null

    /**
     * ボトムバーの項目がクリックされたときのリスナ
     */
    private var onBottomMenuItemClickListener : Listener<UserBottomItem>? = null

    /**
     * ボトムバーの項目がクリックされたときのリスナを設定する
     */
    private fun setOnBottomMenuItemClickListener(listener: Listener<UserBottomItem>?) {
        onBottomMenuItemClickListener = listener
    }

    // ------ //

    val bookmarksActivityLauncher = registerForActivityResult(BookmarksActivityContract()) { entry ->
        entry?.let {
            updateBookmark(it, it.bookmarkedData)
        }
    }

    // ------ //

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        setTheme(Theme.themeId(prefs))

        lifecycleScope.launchWhenCreated {
            runCatching { viewModel.initialize(forceUpdate = false) }
                .onFailure {
                    showToast(R.string.msg_auth_failed)
                    Log.e("error", Log.getStackTraceString(it))
                }
            if (savedInstanceState == null) {
                showContents()
            }
        }

        binding = ActivityEntries2Binding.inflate(layoutInflater).also {
            it.vm = viewModel
            it.lifecycleOwner = this
        }
        setContentView(binding.root)

        // カテゴリリスト初期化
        binding.categoriesList.adapter = CategoriesAdapter().apply {
            setOnItemClickedListener { category ->
                binding.drawerLayout.closeDrawers()
                showCategory(category)
            }
        }

        binding.drawerLayout.setGravity(viewModel.drawerGravity)

        // カテゴリリストのドロワ表示状態切り替えを監視する
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                // IMEを明示的に閉じないと被ってしまう
                hideSoftInputMethod()
            }
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        // エクストラスクロール機能を初期化
        initializeExtraScrollBar()

        // --- Event listeners ---

        // FABメニュー表示ボタン
        binding.entriesMenuButton.setOnClickListener {
            if (isFABMenuOpened) {
                closeFABMenu()
            }
            else {
                openFABMenu()
            }
        }

        // 通知リスト画面表示ボタン
        binding.entriesMenuNoticesButton.setOnClickListener {
            closeFABMenu()
            showCategory(Category.Notices)
        }

        // カテゴリリスト表示ボタン
        binding.entriesMenuCategoriesButton.setOnClickListener {
            closeFABMenu()
            binding.drawerLayout.openDrawer(binding.drawerArea)
        }

        // サインイン/マイブックマークボタン
        binding.entriesMenuMyBookmarksButton.setOnClickListener {
            closeFABMenu()
            if (viewModel.signedIn.value == true) {
                // マイブックマークを表示
                showCategory(Category.MyBookmarks)
            }
            else {
                // サインイン画面に遷移
                val intent = Intent(this, HatenaAuthenticationActivity::class.java)
                startActivity(intent)
            }
        }

        // 設定画面表示ボタン
        binding.entriesMenuPreferencesButton.setOnClickListener {
            closeFABMenu()
            val intent = Intent(this, PreferencesActivity::class.java)
            startActivity(intent)
        }

        // メニューを表示している間の黒背景
        binding.entriesMenuBackgroundGuardFull.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    closeFABMenu()
                    true
                }

                else -> false
            }
        }

        // --- Observers ---

        viewModel.signedIn.observe(this, {
            if (it) {
                binding.entriesMenuNoticesButton.show()
            }
            else {
                binding.entriesMenuNoticesButton.hide()
            }
            binding.entriesMenuNoticesDesc.visibility = it.toVisibility()
        })

        // 通信状態の変更を監視
        SatenaApplication.instance.networkReceiver.state.observe(this, { state ->
            if (state == NetworkReceiver.State.CONNECTED) {
                val needToSignIn = viewModel.signedIn.value != true
                lifecycleScope.launchWhenCreated {
                    runCatching { viewModel.initialize(forceUpdate = false) }
                        .onSuccess {
                            if (needToSignIn) {
                                HatenaClient.account?.name?.let {
                                    showToast(R.string.msg_retry_sign_in_succeeded, it)
                                }
                            }
                        }
                }
            }
        })

        // 非表示エントリ情報が更新されたらリストを更新する
        viewModel.repository.ignoredEntriesRepo.ignoredEntriesForEntries.observe(this, {
            runCatching {
                refreshLists()
            }
        })
    }

    /** 最初に表示するコンテンツの用意 */
    private fun showContents() {
        val user = intent.getStringExtra(EXTRA_USER)
        val siteUrl = intent.getStringExtra(EXTRA_SITE_URL)
        val searchTag = intent.getStringExtra(EXTRA_SEARCH_TAG)
        val openNotices = intent.getBooleanExtra(EXTRA_OPEN_NOTICES, false)

        when {
            openNotices -> showCategory(Category.Notices)

            user != null -> showUserEntries(user)

            siteUrl != null -> showSiteEntries(siteUrl)

            searchTag != null -> showSearchEntries(searchTag, SearchType.Tag)

            else -> {
                val category =
                    if (viewModel.signedIn.value != true && viewModel.homeCategory.requireSignedIn) {
                        showToast(R.string.msg_force_default_home_category)
                        Category.All
                    }
                    else viewModel.homeCategory

                showCategory(category)
            }
        }

        // アップデート後最初の起動時に更新履歴を表示
        showReleaseNotes()
    }

    /** 必要ならリリースノートを表示する */
    private fun showReleaseNotes() {
        // アプリのバージョン名を取得
        val currentVersionName = SatenaApplication.instance.versionName

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        val lastVersionName = prefs.getString(PreferenceKey.APP_VERSION_LAST_LAUNCH)
        val showReleaseNotes = prefs.getBoolean(PreferenceKey.SHOW_RELEASE_NOTES_AFTER_UPDATE)

        prefs.edit {
            putString(PreferenceKey.APP_VERSION_LAST_LAUNCH, currentVersionName)
        }

        if (showReleaseNotes && lastVersionName != null && lastVersionName != currentVersionName) {
            val dialog = ReleaseNotesDialogFragment.createInstance(lastVersionName, currentVersionName)
            dialog.showAllowingStateLoss(supportFragmentManager, DIALOG_RELEASE_NOTES)
        }
    }

    override fun onResume() {
        super.onResume()

        // アプリ内アップデートを使用する
        viewModel.startAppUpdate(this, binding.snackBarArea, REQUEST_CODE_UPDATE)

        // レイアウトモード反映
        binding.bottomAppBar.let {
            it.visibility = viewModel.isBottomLayoutMode.toVisibility(View.INVISIBLE)
            // 下部バー利用中の場合、設定によってはスクロールで隠す
            it.hideOnScroll = viewModel.isBottomLayoutMode && viewModel.hideBottomAppBarByScroll
        }
        binding.bottomSearchView.let {
            it.visibility = (it.visibility == View.VISIBLE && viewModel.isBottomLayoutMode).toVisibility()
        }

        // ツールバーを隠す設定を反映
        binding.toolbar.let {
            it.updateLayoutParams<AppBarLayout.LayoutParams> {
                scrollFlags =
                    if (viewModel.hideToolbarByScroll) AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    else 0
            }

            // アクションバー設定
            setSupportActionBar(it)
        }

        // カテゴリリストの表示形式を適用
        binding.categoriesList.let {
            val categoriesAdapter = it.adapter as? CategoriesAdapter
            when (viewModel.categoriesMode) {
                CategoriesMode.LIST -> {
                    it.layoutManager = LinearLayoutManager(this)
                    categoriesAdapter?.updateLayout(R.layout.listview_item_categories)
                }

                CategoriesMode.GRID -> {
                    it.layoutManager = GridLayoutManager(this, 4)
                    categoriesAdapter?.updateLayout(R.layout.listview_item_categories_grid)
                }
            }
        }

        binding.motionLayout.post {
            updateExtraScrollBarLayout(binding.motionLayout, viewModel.extraScrollingAlignment)
        }

        // 画面遷移後や復元後にツールバーを強制的に再表示する
        showAppBar()

        // ドロワを配置
        binding.drawerArea.updateLayoutParams<DrawerLayout.LayoutParams> {
            gravity = viewModel.drawerGravity
        }
    }

    override fun onRestart() {
        super.onRestart()

        // アカウントの状態を更新する
        lifecycleScope.launchWhenCreated {
            runCatching { viewModel.initialize(forceUpdate = true) }
                .onFailure {
                    showToast(R.string.msg_auth_failed)
                    Log.e("error", Log.getStackTraceString(it))
                }
        }
    }

    /** 戻るボタンの挙動 */
    override fun onBackPressed() {
        when {
            isDrawerOpened -> binding.drawerLayout.closeDrawer(binding.drawerArea)

            isFABMenuOpened -> closeFABMenu()

            extraScrolling -> binding.motionLayout.transitionToStart()

            else -> finish()
        }
    }

    /** Activity遷移で結果が返ってくるのを期待する場合 */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_UPDATE -> {
                // アプリアップデートから戻ってきた
                if (resultCode != RESULT_OK) {
                    Log.e("AppUpdate", "update failed. code: $resultCode")
                    showToast(R.string.msg_app_update_failed)
                }
            }

            BookmarkPostActivity.REQUEST_CODE -> {
                // ブクマ投稿ダイアログから戻ってきた
                if (resultCode == RESULT_OK && data != null) {
                    val entry = data.getObjectExtra<Entry>(BookmarkPostActivity.RESULT_ENTRY) ?: return
                    val bookmarkResult = data.getObjectExtra<BookmarkResult>(BookmarkPostActivity.RESULT_BOOKMARK) ?: return

                    updateBookmark(entry, bookmarkResult)
                }
            }
        }
    }

    /** (カテゴリメニューから遷移できる)カテゴリを選択 */
    private fun showCategory(category: Category) = viewModel.showCategory(this, category)

    /** Category.Siteに遷移 */
    fun showSiteEntries(siteUrl: String) = viewModel.showSiteEntries(this, siteUrl)

    /** Category.Userに遷移 */
    fun showUserEntries(user: String) = viewModel.showUserEntries(this, user)

    /** Category.Search */
    fun showSearchEntries(query: String, searchType: SearchType) = viewModel.showSearchEntries(this, query, searchType)

    /** AppBarを強制的に表示する */
    fun showAppBar() {
        runCatching {
            binding.appbarLayout.setExpanded(true, true)
            if (viewModel.isBottomLayoutMode) {
                binding.bottomAppBar.performShow()
            }
        }
    }

    /** カテゴリリストドロワを表示する */
    fun openDrawer() {
        runCatching {
            binding.drawerLayout.openDrawer(binding.drawerArea)
        }
    }

    /**
     * 表示中のフラグメントの内容を一番上までスクロールする
     */
    override fun scrollToTop() {
        val fragment = supportFragmentManager.get<EntriesFragment>()
        fragment?.scrollToTop()
        showAppBar()
    }

    /** エントリリストを再構成する */
    fun refreshLists() {
        val fragment = supportFragmentManager.get<EntriesFragment>()
        fragment?.refreshLists()
    }

    /** エントリに付けたブクマを削除する */
    fun removeBookmark(entry: Entry) {
        val fragment = supportFragmentManager.get<EntriesFragment>()
        fragment?.removeBookmark(entry)
    }

    /** エントリに付けたブクマを更新する */
    fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult?) {
        val fragment = supportFragmentManager.get<EntriesFragment>()
        fragment?.updateBookmark(entry, bookmarkResult)
    }

    /** アクティビティ終了時確認 */
    override fun finish() {
        if (viewModel.isTerminationDialogEnabled && supportFragmentManager.backStackEntryCount <= 1) {
            val dialog = AlertDialogFragment.Builder()
                .setTitle(R.string.confirm_dialog_title_simple)
                .setMessage(R.string.app_termination_dialog_msg)
                .setNegativeButton(R.string.dialog_cancel)
                .setPositiveButton(R.string.dialog_ok) {
                    finishImpl()
                }
                .create()

            dialog.showAllowingStateLoss(supportFragmentManager, DIALOG_TERMINATION) { e ->
                Log.e("TerminationDialog", Log.getStackTraceString(e))
                showToast(R.string.msg_termination_dialog_error)
                finishImpl()
            }
        }
        else {
            finishImpl()
        }
    }

    private fun finishImpl() {
        try {
            super.onBackPressed()
            if (supportFragmentManager.backStackEntryCount == 0) {
                super.finish()
            }
        }
        catch (e: Throwable) {
            Log.e("finish", Log.getStackTraceString(e))
        }
    }

    /** タブの状態を初期化する */
    private fun clearTabLayoutState(tabLayout: TabLayout) {
        tabLayout.let {
            // デフォルトではタブをスクロール不可にしておく
            it.tabMode = TabLayout.MODE_FIXED

            it.setupWithViewPager(null)
            it.clearOnTabSelectedListeners()
            it.setOnLongClickListener(null)
        }
    }

    /** タブを初期化して渡す */
    fun initializeTabLayout() : TabLayout = binding.topTabLayout.also {
        clearTabLayoutState(it)
    }

    /** ボトムバーの状態を初期化する */
    private fun clearBottomAppBarState(bottomAppBar: BottomAppBar) {
        bottomAppBar.menu.clear()
        bottomAppBar.setOnMenuItemClickListener(null)
        binding.bottomSearchView.visibility = View.GONE
        bottomAppBar.alsoAs<CustomBottomAppBar> {
            it.bindMenuItemsGravity(viewModel.bottomBarItemsGravity)
        }

        inflateBasicBottomItems(bottomAppBar)
    }

    /** 基本のボトムバーアイテムを追加する */
    private fun inflateBasicBottomItems(bottomAppBar: BottomAppBar) {
        // 今現在の画面に表示できる最大数を計算し、設定されたアイテムをこの数に絞る
        val maxButtonsNum = UserBottomItemsSetter.getButtonsLimit(this)

        val tint = ColorStateList.valueOf(getThemeColor(R.attr.textColor))
        val menuItems = viewModel.bottomBarItems
            .take(maxButtonsNum)
            .mapNotNull { item ->
                val result = runCatching {
                    if (item.requireSignedIn && viewModel.signedIn.value != true) null
                    else item.toMenuItem(bottomAppBar.menu, tint)
                }
                result.getOrNull()?.also { menuItem ->
                    initializeBottomMenuItemActionView(item, menuItem)
                }
            }

        bottomAppBar.setOnMenuItemClickListener { clicked ->
            val idx = menuItems.indexOf(clicked)
            if (idx != -1) {
                val item = viewModel.bottomBarItems[idx]
                onBottomMenuItemClickListener?.invoke(item)
            }
            true
        }

        setOnBottomMenuItemClickListener { item ->
            viewModel.onBasicBottomMenuItemClicked(this, item)
        }
    }

    /**
     * ボトムメニュー項目をロングタップ可能にするための置換処理
     */
    private fun initializeBottomMenuItemActionView(item: UserBottomItem, menuItem: MenuItem) {
        if (!item.longClickable) return

        menuItem.actionView = ImageButton(this).apply {
            setImageResource(item.iconId)
            imageTintList = ColorStateList.valueOf(getThemeColor(R.attr.textColor))
            background = getThemeDrawable(R.attr.actionBarItemBackground)

            setOnClickListener {
                onBottomMenuItemClickListener?.invoke(item)
            }

            setOnLongClickListener {
                viewModel.onBottomMenuItemLongClicked(this@EntriesActivity, item)
                true
            }

            val entireSize = dp2px(48)
            layoutParams = ViewGroup.LayoutParams(entireSize, entireSize)
        }
    }

    /** ボトムバーを使用する設定なら取得する(使用しない設定ならnullが返る) */
    fun initializeBottomAppBar() : BottomAppBar? =
        if (viewModel.isBottomLayoutMode) binding.bottomAppBar.also { clearBottomAppBarState(it) }
        else null

    /** ボトムバーにメニューアイテムを追加する */
    fun inflateExtraBottomMenu(@MenuRes menuId: Int) {
        if (!viewModel.isBottomLayoutMode) return

        val prefValue = viewModel.extraBottomItemsAlignment

        val alignment =
            if (prefValue == ExtraBottomItemsAlignment.DEFAULT) {
                when (viewModel.bottomBarItemsGravity) {
                    Gravity.END -> ExtraBottomItemsAlignment.LEFT
                    Gravity.START -> ExtraBottomItemsAlignment.RIGHT
                    else -> throw NotImplementedError()
                }
            }
            else prefValue

        when (alignment) {
            ExtraBottomItemsAlignment.RIGHT ->
                binding.bottomAppBar.inflateMenu(menuId)

            ExtraBottomItemsAlignment.LEFT -> {
                binding.bottomAppBar.let {
                    it.menu.clear()
                    it.inflateMenu(menuId)
                    inflateBasicBottomItems(it)
                }
            }

            else -> throw NotImplementedError()
        }
    }

    /** コンテンツ部分にMotionLayoutを導入したことでツールバー開閉が暗黙的に行えなくなったため、明示的に呼び出す */
    fun updateScrollBehavior(dx: Int, dy: Int) {
        // ツールバーを開閉
        binding.appbarLayout.layoutParams.alsoAs<CoordinatorLayout.LayoutParams> { params ->
            val behavior = params.behavior as? AppBarLayout.Behavior ?: return@alsoAs
            behavior.onNestedPreScroll(
                binding.mainContentLayout,
                binding.appbarLayout,
                binding.mainLayout,
                dx, dy, IntArray(2), ViewCompat.TYPE_TOUCH
            )
        }
        // ボトムバーを開閉
        if (viewModel.hideBottomAppBarByScroll) {
            binding.bottomAppBar.layoutParams.alsoAs<CoordinatorLayout.LayoutParams> { params ->
                val behavior = params.behavior as? BottomAppBar.Behavior ?: return@alsoAs
                behavior.onNestedScroll(
                    binding.mainContentLayout,
                    binding.bottomAppBar,
                    binding.mainLayout,
                    dx, dy, dx, dy, ViewCompat.TYPE_TOUCH, IntArray(2)
                )
            }
        }
    }

    private val extraScrollingTileHeight : Int
        get() = dp2px(112)

    private val extraMargin : Int
        get() = binding.mainLayout.measuredHeight - extraScrollingTileHeight * 3

    private fun initializeExtraScrollBar() {
        binding.motionLayout.addTransitionListener(object : MotionLayout.TransitionListener {
            private val duration = 500L

            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
                binding.mainLayout.updatePadding(top = (extraMargin * progress).toInt())
            }
            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                binding.mainLayout.updatePadding(
                    top = when (currentId) {
                        R.id.end -> extraMargin
                        else -> 0
                    }
                )

                val bgView = binding.extraScrollBackground
                bgView.animate()
                    .withEndAction { bgView.visibility = View.GONE }
                    .alpha(0.0f)
                    .setDuration(duration)
                    .start()
            }
            override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {
                val bgView = binding.extraScrollBackground
                bgView.animate()
                    .withStartAction {
                        bgView.alpha = 0.0f
                        bgView.visibility = View.VISIBLE
                    }
                    .alpha(1.0f)
                    .setDuration(duration)
                    .start()
            }
            override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {}

            init {
                onTransitionCompleted(binding.motionLayout, 0)
            }
        })
    }

    private fun updateExtraScrollBarLayout(motionLayout: MotionLayout, alignment: ExtraScrollingAlignment?) {
        viewModel.updateExtraScrollBarVisibility(extraMargin > extraScrollingTileHeight)

        val margin = dp2px(38)
        val edge =
            if (alignment == ExtraScrollingAlignment.LEFT) ConstraintSet.LEFT
            else ConstraintSet.RIGHT

        val horizontalInitializer : (ConstraintSet)->Unit = { set ->
            set.clear(R.id.extra_scroll_thumb, ConstraintSet.LEFT)
            set.clear(R.id.extra_scroll_thumb, ConstraintSet.RIGHT)
            set.connect(R.id.extra_scroll_thumb, edge, ConstraintSet.PARENT_ID, edge, margin)
        }
        motionLayout.getConstraintSet(R.id.start).let(horizontalInitializer)
        motionLayout.getConstraintSet(R.id.end).let(horizontalInitializer)
        motionLayout.requestLayout()
        motionLayout.transitionToStart()
    }

    // --- FAB表示アニメーション ---

    /** FABメニュー各項目のオープン時移動アニメーション */
    private fun openFABMenuAnimation(fab: FloatingActionButton, desc: View, dimenId: Int) {
        val metrics = resources.displayMetrics

        fab.visibility = View.VISIBLE
        fab.animate()
            .withEndAction {
                val descWidth = desc.width / 2f
                desc.animate()
                    .translationXBy(0f)
                    .translationX(-descWidth - (8f * metrics.density))
                    .alphaBy(0.0f)
                    .alpha(1.0f)
                    .duration = 100
            }
            .translationY(-resources.getDimension(dimenId))
            .alphaBy(0.0f)
            .alpha(1.0f)
            .duration = 100
    }

    /** FABメニュー各項目のクローズ時移動アニメーション */
    private fun closeFABMenuAnimation(fab: FloatingActionButton, desc: View) {
        if (fab.visibility != View.VISIBLE) return

        desc.animate()
            .withEndAction {
                fab.animate()
                    .withEndAction {
                        fab.visibility = View.INVISIBLE
                    }
                    .translationY(0f)
                    .alphaBy(1.0f)
                    .alpha(0.0f)
                    .duration = 100
            }
            .translationX(100f)
            .alphaBy(1.0f)
            .alpha(0.0f)
            .duration = 100
    }


    /** FABメニューを開く */
    private fun openFABMenu() {
        if (isFABMenuOpened) return

        // メニュー表示中はIMEを隠す
        hideSoftInputMethod()

        isFABMenuOpened = true

        if (viewModel.isFABMenuBackgroundActive) {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            binding.entriesMenuBackgroundGuardFull.visibility = View.VISIBLE
        }

        binding.entriesMenuBackgroundGuard.visibility = View.VISIBLE

        if (viewModel.signedIn.value == true) {
            openFABMenuAnimation(
                binding.entriesMenuNoticesButton,
                binding.entriesMenuNoticesDesc,
                R.dimen.dp_238
            )
        }
        openFABMenuAnimation(
            binding.entriesMenuCategoriesButton,
            binding.entriesMenuCategoriesDesc,
            R.dimen.dp_180
        )
        openFABMenuAnimation(
            binding.entriesMenuMyBookmarksButton,
            binding.entriesMenuMyBookmarksDesc,
            R.dimen.dp_122
        )
        openFABMenuAnimation(
            binding.entriesMenuPreferencesButton,
            binding.entriesMenuPreferencesDesc,
            R.dimen.dp_64
        )

        binding.entriesMenuButton.setImageResource(R.drawable.ic_baseline_close)
    }

    /** FABメニューを閉じる */
    private fun closeFABMenu() {
        if (!isFABMenuOpened) return

        isFABMenuOpened = false
        binding.entriesMenuBackgroundGuardFull.visibility = View.GONE
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        binding.entriesMenuBackgroundGuard.visibility = View.GONE

        closeFABMenuAnimation(
            binding.entriesMenuNoticesButton,
            binding.entriesMenuNoticesDesc,
        )
        closeFABMenuAnimation(
            binding.entriesMenuCategoriesButton,
            binding.entriesMenuCategoriesDesc,
        )
        closeFABMenuAnimation(
            binding.entriesMenuMyBookmarksButton,
            binding.entriesMenuMyBookmarksDesc,
        )
        closeFABMenuAnimation(
            binding.entriesMenuPreferencesButton,
            binding.entriesMenuPreferencesDesc
        )

        binding.entriesMenuButton.setImageResource(R.drawable.ic_baseline_menu_white)
    }
}

