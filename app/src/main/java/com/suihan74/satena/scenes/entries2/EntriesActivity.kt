package com.suihan74.satena.scenes.entries2

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
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
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.Theme
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity2
import com.suihan74.satena.scenes.entries2.dialog.BrowserShortcutDialog
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.bottomBar.UserBottomItemsSetter
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.views.CustomBottomAppBar
import com.suihan74.utilities.views.bindMenuItemsGravity

class EntriesActivity : AppCompatActivity() {
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
            accountLoader = app.accountLoader,
            ignoredEntriesRepo = app.ignoredEntriesRepository,
            favoriteSitesRepo = app.favoriteSitesRepository
        )
        EntriesViewModel(repository)
    }

    // ------ //

    private lateinit var binding: ActivityEntries2Binding

    val toolbar : Toolbar
        get() = binding.toolbar

    // ------ //

    /** ドロワーの開閉状態 */
    private val isDrawerOpened : Boolean
        get() = binding.drawerLayout.isDrawerOpen(binding.drawerArea)

    /** FABメニューの開閉状態 */
    private var isFABMenuOpened : Boolean = false

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
    fun setOnBottomMenuItemClickListener(listener: Listener<UserBottomItem>?) {
        onBottomMenuItemClickListener = listener
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        setTheme(Theme.themeId(prefs))

        viewModel.initialize(
            lifecycleScope,
            forceUpdate = false,
            onFinally = {
                if (savedInstanceState == null) {
                    showContents()
                }
            },
            onError = { e ->
                showToast(R.string.msg_auth_failed)
                Log.e("error", Log.getStackTraceString(e))
            }
        )

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
                val intent = Intent(this, HatenaAuthenticationActivity2::class.java)
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

        viewModel.signedIn.observe(this, Observer {
            if (it) {
                binding.entriesMenuNoticesButton.show()
            }
            else {
                binding.entriesMenuNoticesButton.hide()
            }
            binding.entriesMenuNoticesDesc.visibility = it.toVisibility()
        })

        // 通信状態の変更を監視
        SatenaApplication.instance.networkReceiver.state.observe(this, Observer { state ->
            if (state == NetworkReceiver.State.CONNECTED) {
                val needToSignIn = viewModel.signedIn.value != true
                viewModel.initialize(
                    lifecycleScope,
                    forceUpdate = false,
                    onSuccess = {
                        if (needToSignIn) {
                            val accountName = HatenaClient.account?.name ?: return@initialize
                            showToast(R.string.msg_retry_sign_in_succeeded, accountName)
                        }
                    }
                )
            }
        })

        // 非表示エントリ情報が更新されたらリストを更新する
        viewModel.repository.ignoredEntriesRepo.ignoredEntriesForEntries.observe(this, Observer {
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
        viewModel.initialize(
            lifecycleScope,
            forceUpdate = false,
            onError = { e ->
                showToast(R.string.msg_auth_failed)
                Log.e("error", Log.getStackTraceString(e))
            }
        )
    }

    /** 戻るボタンの挙動 */
    override fun onBackPressed() {
        when {
            isDrawerOpened -> binding.drawerLayout.closeDrawer(binding.drawerArea)

            isFABMenuOpened -> closeFABMenu()

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
    private fun showCategory(category: Category) {
        showContentFragment(category) {
            category.createFragment()
        }
    }

    /** Category.Siteに遷移 */
    fun showSiteEntries(siteUrl: String) {
        showContentFragment(Category.Site) {
            Category.Site.createSiteFragment(siteUrl)
        }
    }

    /** Category.Userに遷移 */
    fun showUserEntries(user: String) {
        showContentFragment(Category.User) {
            Category.User.createUserFragment(user)
        }
    }

    /** Category.Search */
    fun showSearchEntries(query: String, searchType: SearchType) {
        showContentFragment(Category.Search) {
            Category.Search.createSearchFragment(query, searchType)
        }
    }

    /** EntriesFragment遷移処理 */
    private fun showContentFragment(category: Category, fragmentGenerator: ()->EntriesFragment) {
        // 現在トップにある画面と同じカテゴリには連続して遷移しない
        if (supportFragmentManager.topBackStackEntry?.name == category.name) return

        // 既に一度表示されているカテゴリの場合再利用する
        val existed = supportFragmentManager.findFragmentByTag(category.name)
        val fragment = existed ?: fragmentGenerator()

        supportFragmentManager.beginTransaction().run {
            replace(R.id.main_layout, fragment, category.name)
            addToBackStack(category.name)
            commitAllowingStateLoss()
        }
    }

    /** AppBarを強制的に表示する */
    fun showAppBar() {
        binding.appbarLayout.setExpanded(true, true)
        if (viewModel.isBottomLayoutMode) {
            binding.bottomAppBar.performShow()
        }
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
    fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult) {
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

        setOnBottomMenuItemClickListener(::onBasicBottomMenuItemClicked)
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
                onBasicBottomMenuItemLongClicked(item)
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

    /** (基本の)ボトムバーアイテムを選択した際の処理 */
    private fun onBasicBottomMenuItemClicked(item: UserBottomItem) = when (item) {
        UserBottomItem.SCROLL_TO_TOP -> {
            val fragment = supportFragmentManager.get<EntriesFragment>()
            fragment?.scrollToTop()
            showAppBar()
        }

        UserBottomItem.MYBOOKMARKS -> showCategory(Category.MyBookmarks)

        UserBottomItem.NOTICE -> showCategory(Category.Notices)

        UserBottomItem.INNER_BROWSER -> startInnerBrowser()

        UserBottomItem.SEARCH -> showCategory(Category.Search)

        UserBottomItem.HOME -> showCategory(viewModel.homeCategory)

        UserBottomItem.PREFERENCES -> {
            val intent = Intent(this, PreferencesActivity::class.java)
            startActivity(intent)
        }

        UserBottomItem.OPEN_OFFICIAL_TOP -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://b.hatena.ne.jp/"))
            startActivity(intent)
        }

        UserBottomItem.OPEN_OFFICIAL_HATENA -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.hatena.ne.jp/"))
            startActivity(intent)
        }

        UserBottomItem.OPEN_ANONYMOUS_DIARY -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://anond.hatelabo.jp/"))
            startActivity(intent)
        }

        UserBottomItem.CATEGORIES -> {
            binding.drawerLayout.openDrawer(binding.drawerArea)
        }
    }

    /**
     * ボトムバーアイテムをロングタップしたときの処理
     *
     * `UserBottomItem#longClickable`が`true`に設定されてるアイテムのみ呼ばれる
     */
    private fun onBasicBottomMenuItemLongClicked(item: UserBottomItem) = when (item) {
        UserBottomItem.INNER_BROWSER -> {
            val dialog = BrowserShortcutDialog.createInstance()
            dialog.showAllowingStateLoss(supportFragmentManager)
        }

        else -> {}
    }

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

