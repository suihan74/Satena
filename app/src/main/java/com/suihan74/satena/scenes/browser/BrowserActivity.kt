package com.suihan74.satena.scenes.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityBrowserBinding
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.browser.bookmarks.BookmarksRepository
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.satena.scenes.post.BookmarkPostRepository
import com.suihan74.satena.scenes.post.BookmarkPostViewModel
import com.suihan74.satena.scenes.post.BookmarkPostViewModelOwner
import com.suihan74.utilities.*
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.extensions.hideSoftInputMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrowserActivity :
    AppCompatActivity(),
    BookmarkPostViewModelOwner
{
    companion object {
        /** 最初に開くページのURL */
        const val EXTRA_URL = "BrowserActivity.EXTRA_URL"
    }

    // ------ //

    val viewModel : BrowserViewModel by lazyProvideViewModel {
        val initialUrl = intent.getStringExtra(EXTRA_URL)

        val app = SatenaApplication.instance
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)

        val browserRepo = BrowserRepository(
            HatenaClient,
            prefs,
            SafeSharedPreferences.create<BrowserSettingsKey>(this)
        )

        val bookmarksRepo = BookmarksRepository(
            AccountLoader(this, HatenaClient, MastodonClientHolder),
            prefs,
            app.ignoredEntriesRepository,
            app.userTagDao
        )

        val historyRepo = HistoryRepository(app.browserDao)

        BrowserViewModel(
            browserRepo,
            bookmarksRepo,
            app.favoriteSitesRepository,
            historyRepo,
            initialUrl
        )
    }

    /** ブクマ投稿用のViewModel */
    override val bookmarkPostViewModel by lazyProvideViewModel {
        val repository = BookmarkPostRepository(
            viewModel.bookmarksRepo.accountLoader,
            viewModel.bookmarksRepo.prefs
        )
        BookmarkPostViewModel(repository)
    }

    // ------ //

    private lateinit var binding : ActivityBrowserBinding

    val webView : WebView
        get() = binding.webview

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(viewModel.themeId)

        val binding = DataBindingUtil.setContentView<ActivityBrowserBinding>(
            this,
            R.layout.activity_browser
        ).apply {
            vm = viewModel
            lifecycleOwner = this@BrowserActivity
        }
        this.binding = binding

        // ドロワの設定
        initializeDrawer()

        // ボトムシートの設定
        initializeBottomSheet()

        // WebViewの設定
        viewModel.initializeWebView(binding.webview, this)

        // ツールバーの設定
        initializeToolbar()

        // スワイプしてページを更新する
        binding.swipeLayout.let { swipeLayout ->
            swipeLayout.setProgressBackgroundColorSchemeColor(getThemeColor(R.attr.swipeRefreshBackground))
            swipeLayout.setColorSchemeColors(getThemeColor(R.attr.colorPrimary))
            swipeLayout.setOnRefreshListener {
                viewModel.setOnPageFinishedListener {
                    swipeLayout.isRefreshing = false
                    viewModel.setOnPageFinishedListener(null)
                }
                binding.webview.reload()
            }
        }
    }

    /** 戻る処理を制御する */
    override fun onBackPressed() {
        when {
            onBackPressedDispatcher.hasEnabledCallbacks() -> {
                // タブの戻るボタン割り込み
                onBackPressedDispatcher.onBackPressed()
            }

            bottomSheetOpened -> {
                // ボトムシートを閉じる
                closeBottomSheet()
            }

            drawerOpened -> {
                // ドロワを閉じる
                closeDrawer()
            }

            webView.canGoBack() -> {
                // ページを戻る
                webView.goBack()
            }

            // Activityを終了する
            else -> super.onBackPressed()
        }
    }

    /** 戻るボタン長押しで「戻る/進む」履歴リストを表示する */
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                switchBottomSheetState()
                true
            }
            else -> super.onKeyLongPress(keyCode, event)
        }
    }

    override fun onResume() {
        super.onResume()

        binding.drawerArea.updateLayoutParams<DrawerLayout.LayoutParams> {
            gravity = viewModel.drawerGravity
        }
    }

    override fun onRestart() {
        super.onRestart()
        // 別画面で更新されている可能性があるキャッシュを再読み込みする
        viewModel.viewModelScope.launch(Dispatchers.Default) {
            viewModel.bookmarksRepo.onRestart()
        }
    }

    override fun finish() {
        // リソースの読み込み、スクリプトの実行をすべて中断させる
        // https://developer.android.com/reference/android/webkit/WebView.html#clearView()
        binding.webview.run {
            stopLoading()
            loadUrl("about:blank")
        }

        super.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.browser, menu)
        if (menu != null) {
            viewModel.bindOptionsMenu(this, this, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return viewModel.onOptionsItemSelected(item, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(this, requestCode, resultCode, data)
    }

    // ------ //

    /** ボトムシートの表示状態を切り替える */
    @MainThread
    fun switchBottomSheetState() {
        if (bottomSheetOpened) {
            closeBottomSheet()
        }
        else {
            openBottomSheet()
        }
    }

    /** ボトムシートを開く */
    @MainThread
    fun openBottomSheet() {
        if (viewModel.drawerOpened.value != true) {
            val behavior = BottomSheetBehavior.from(binding.bottomSheetLayout)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            viewModel.bottomSheetOpened.value = true
        }
    }

    /** ボトムシートを閉じる */
    @MainThread
    fun closeBottomSheet() {
        val behavior = BottomSheetBehavior.from(binding.bottomSheetLayout)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        viewModel.bottomSheetOpened.value = false
    }

    /** ボトムシートが開かれているか */
    val bottomSheetOpened : Boolean
        get() = when (BottomSheetBehavior.from(binding.bottomSheetLayout).state) {
            BottomSheetBehavior.STATE_COLLAPSED,
            BottomSheetBehavior.STATE_HIDDEN -> false

            else -> true
        }

    // ------ //

    /** ドロワを開いて設定タブを表示する */
    @MainThread
    fun showPreferencesFragment() {
        binding.drawerViewPager.currentItem = DrawerTab.SETTINGS.ordinal
        openDrawer()
    }

    /** ドロワを開く */
    @MainThread
    fun openDrawer() {
        lifecycleScope.launchWhenResumed {
            binding.drawerLayout.openDrawer(binding.drawerArea)
            viewModel.drawerOpened.value = true
        }
    }

    /**
     * ドロワを閉じる
     *
     * BrowserActivityに依存するフラグメント側から閉じるために使用
     */
    @MainThread
    fun closeDrawer() {
        lifecycleScope.launchWhenResumed {
            binding.drawerLayout.closeDrawer(binding.drawerArea)
            viewModel.drawerOpened.value = false
        }
    }

    /** ドロワが開かれている */
    val drawerOpened : Boolean
        get() = binding.drawerLayout.isDrawerOpen(binding.drawerArea)

    // ------ //

    /**
     * URLを開く
     *
     * BrowserActivityに依存するフラグメント側からページ遷移するために使用
     */
    @MainThread
    fun openUrl(url: String) {
        viewModel.goAddress(url)
    }

    // ------ //

    /**
     * ドロワの挙動を設定する
     */
    @MainThread
    fun initializeDrawer() {
        val drawerLayout = binding.drawerLayout
        val drawerTabLayout = binding.drawerTabLayout
        val drawerViewPager = binding.drawerViewPager
        val mainArea = binding.mainArea
        val drawerArea = binding.drawerArea

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                viewModel.drawerOpened.value = true
                drawerViewPager.adapter?.alsoAs<DrawerTabAdapter> { adapter ->
                    val position = drawerTabLayout.selectedTabPosition
                    adapter.findFragment(drawerViewPager, position)?.alsoAs<TabItem> { fragment ->
                        fragment.onTabSelected()
                    }
                }
            }
            override fun onDrawerClosed(drawerView: View) {
                viewModel.drawerOpened.value = false
                // 閉じたことをドロワタブに通知する
                drawerViewPager.adapter?.alsoAs<DrawerTabAdapter> { adapter ->
                    val position = drawerTabLayout.selectedTabPosition
                    adapter.findFragment(drawerViewPager, position)?.alsoAs<TabItem> { fragment ->
                        fragment.onTabUnselected()
                    }
                }
            }
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {
                // ドロワ開閉でIMEを閉じる
                hideSoftInputMethod(mainArea)
            }
        })

        val drawerTabAdapter = DrawerTabAdapter(supportFragmentManager)
        drawerTabAdapter.setup(this, drawerTabLayout, drawerViewPager)
        drawerTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            @SuppressLint("RtlHardcoded")
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // ドロワ内のタブ切り替え操作と干渉するため
                // 一番端のタブを表示中以外はスワイプで閉じないようにする
                val closerEnabled = when (viewModel.drawerGravity) {
                    Gravity.LEFT -> tab?.position == drawerTabAdapter.count - 1
                    Gravity.RIGHT -> tab?.position == 0
                    else -> true // not implemented gravity
                }
                drawerLayout.setCloseSwipeEnabled(closerEnabled, drawerArea)

                val position = tab?.position ?: return
                drawerTabAdapter.findFragment(drawerViewPager, position)?.alsoAs<TabItem> { fragment ->
                    fragment.onTabSelected()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                drawerTabAdapter.findFragment(drawerViewPager, position)?.alsoAs<TabItem> { fragment ->
                    fragment.onTabUnselected()
                }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                drawerTabAdapter.findFragment(drawerViewPager, position)?.alsoAs<TabItem> { fragment ->
                    fragment.onTabReselected()
                }
            }
        })

        viewModel.drawerOpened.observe(this, {
            if (it) openDrawer()
            else closeDrawer()
        })
    }

    /**
     * ボトムシートを設定する
     */
    @SuppressLint("ClickableViewAccessibility")
    @MainThread
    fun initializeBottomSheet() {
        // 「戻る/進む」履歴を表示する
        binding.bottomSheetBackStack.adapter = BackStackAdapter(viewModel, this).also { adapter ->
            adapter.setOnClickItemListener { binding ->
                val url = binding.item?.url ?: return@setOnClickItemListener
                viewModel.goAddress(url)
            }

            adapter.setOnLongLickItemListener { binding ->
                val item = binding.item ?: return@setOnLongLickItemListener
                viewModel.openBackStackItemMenuDialog(this, item, supportFragmentManager)
            }
        }

        val behavior = BottomSheetBehavior.from(binding.bottomSheetLayout)

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            // ボトムシート表示状態ではドロワを表示できないようにする
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val drawerLayout = binding.drawerLayout

                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED,
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        viewModel.bottomSheetOpened.value = false
                        closeBottomSheet()
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    }

                    else -> {
                        viewModel.bottomSheetOpened.value = true
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    }
                }
            }
        })

        viewModel.bottomSheetOpened.observe(this, {
            if (it) openBottomSheet()
            else closeBottomSheet()
        })

        // クリック防止ビュー
        binding.bottomSheetClickGuard.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action != MotionEvent.ACTION_UP) return@setOnTouchListener false
            closeBottomSheet()
            return@setOnTouchListener true
        }
    }

    /**
     * ツールバーを設定する
     */
    @MainThread
    @SuppressLint("ClickableViewAccessibility")
    fun initializeToolbar() {
        val toolbar = BrowserToolbar(this).also { toolbar ->
            // 入力状態になったらwebview部分にクリック防止ビューを被せる
            toolbar.setOnFocusChangeListener { b ->
                binding.clickGuard.setVisibility(b)
            }

            // お気に入りに追加
            toolbar.setOnFavoriteCurrentPageListener {
                viewModel.favoriteCurrentPage(supportFragmentManager)
            }

            // お気に入りから除外
            toolbar.setOnUnfavoriteCurrentPageListener {
                viewModel.unfavoriteCurrentPage(supportFragmentManager)
            }
        }

        viewModel.useBottomAppBar.observe(this) {
            val appbarLayout = binding.appbarLayout
            val bottomAppBar = binding.bottomAppBar

            // 使わない方のツールバーをクリアしておく
            val another =
                if (it) appbarLayout
                else bottomAppBar
            another.removeAllViews()

            val appBar =
                if (it) bottomAppBar
                else appbarLayout

            val toolbarBinding = toolbar.inflate(viewModel, this, appBar, true)
            setSupportActionBar(toolbarBinding.toolbar)
        }

        // クリック防止ビュー
        binding.clickGuard.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action != MotionEvent.ACTION_UP) return@setOnTouchListener false
            hideSoftInputMethod(binding.mainArea)
            return@setOnTouchListener true
        }
    }
}
