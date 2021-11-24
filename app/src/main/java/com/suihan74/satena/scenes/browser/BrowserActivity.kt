package com.suihan74.satena.scenes.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityBrowserBinding
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.satena.scenes.post.BookmarkPostRepository
import com.suihan74.satena.scenes.post.BookmarkPostViewModel
import com.suihan74.satena.scenes.post.BookmarkPostViewModelOwner
import com.suihan74.utilities.*
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrowserActivity :
    AppCompatActivity(),
    BookmarkPostViewModelOwner,
    DrawerOwner {
    companion object {
        /** 最初に開くページのURL */
        const val EXTRA_URL = "BrowserActivity.EXTRA_URL"
    }

    // ------ //

    val viewModel: BrowserViewModel by lazyProvideViewModel {
        val initialUrl = intent.getStringExtra(EXTRA_URL)

        val app = SatenaApplication.instance
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        val browserSettings = SafeSharedPreferences.create<BrowserSettingsKey>(this)

        val browserRepo = BrowserRepository(
            HatenaClient,
            prefs,
            browserSettings
        )

        val bookmarksRepo = BookmarksRepository(
            app.accountLoader,
            prefs,
            SafeSharedPreferences.create(this),
            app.ignoredEntriesRepository,
            app.userTagDao,
            app.readEntryDao
        )

        val historyRepo = HistoryRepository(browserSettings, app.browserDao)

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

    private lateinit var binding: ActivityBrowserBinding

    val webView: WebView
        get() = binding.webview

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(viewModel.themeId)

        binding = ActivityBrowserBinding.inflate(layoutInflater).also {
            it.vm = viewModel
            it.lifecycleOwner = this@BrowserActivity
        }
        setContentView(binding.root)

        // ドロワの設定
        initializeDrawer()

        // ボトムシートの設定
        //initializeBottomSheet()

        // WebViewの設定
        viewModel.initializeWebView(binding.webview, this)

        // ツールバーの設定
        initializeToolbar()

        // スワイプしてページを更新する
        binding.swipeLayout.setOnRefreshListener {
            viewModel.setOnPageFinishedListener {
                binding.swipeLayout.isRefreshing = false
                viewModel.setOnPageFinishedListener(null)
            }
            binding.webview.reload()
        }
    }

    /** 戻る処理を制御する */
    override fun onBackPressed() {
        when {
            onBackPressedDispatcher.hasEnabledCallbacks() -> {
                // タブの戻るボタン割り込み
                onBackPressedDispatcher.onBackPressed()
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
//                switchBottomSheetState()
                viewModel.openBackStackDialog(supportFragmentManager)
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
        binding.webview.finish()
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

    // ------ //

    /** ドロワを開いて設定タブを表示する */
    @MainThread
    fun showPreferencesFragment() {
        binding.drawerViewPager.currentItem = DrawerTab.SETTINGS.ordinal
        openDrawer()
    }

    /** ドロワを開く */
    @MainThread
    override fun openDrawer() {
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
    override fun closeDrawer() {
        lifecycleScope.launchWhenResumed {
            binding.drawerLayout.closeDrawer(binding.drawerArea)
            viewModel.drawerOpened.value = false
        }
    }

    /** ドロワが開かれている */
    private val drawerOpened: Boolean
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

        drawerLayout.setGravity(viewModel.drawerGravity)

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                viewModel.drawerOpened.value = true
                drawerViewPager.adapter?.alsoAs<DrawerTabAdapter> { adapter ->
                    val position = drawerTabLayout.selectedTabPosition
                    adapter.findFragment(position)?.alsoAs<TabItem> { fragment ->
                        fragment.onTabSelected()
                    }
                }
            }

            override fun onDrawerClosed(drawerView: View) {
                viewModel.drawerOpened.value = false
                // 閉じたことをドロワタブに通知する
                drawerViewPager.adapter?.alsoAs<DrawerTabAdapter> { adapter ->
                    val position = drawerTabLayout.selectedTabPosition
                    adapter.findFragment(position)?.alsoAs<TabItem> { fragment ->
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

        val drawerTabAdapter = DrawerTabAdapter(this)
        drawerTabAdapter.setup(this, drawerTabLayout, drawerViewPager)
        drawerTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab == null) return
                viewModel.currentDrawerTab.value = DrawerTab.fromOrdinal(tab.position)

                // ドロワ内のタブ切り替え操作と干渉するため
                // 一番端のタブを表示中以外はスワイプで閉じないようにする
                setDrawerSwipeClosable(tab.position)

                drawerTabAdapter.findFragment(tab.position)?.alsoAs<TabItem> { fragment ->
                    fragment.onTabSelected()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                drawerTabAdapter.findFragment(position)?.alsoAs<TabItem> { fragment ->
                    fragment.onTabUnselected()
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                drawerTabAdapter.findFragment(position)?.alsoAs<TabItem> { fragment ->
                    fragment.onTabReselected()
                }
            }
        })

        viewModel.drawerOpened.observe(this, {
            if (it) openDrawer()
            else closeDrawer()
        })

        val defaultTouchSlop = drawerViewPager.touchSlop
        setDrawerViewPagerSensitivity(drawerViewPager, defaultTouchSlop)
        setDrawerSwipeClosable(drawerTabLayout.selectedTabPosition)

        viewModel.drawerPagerTouchSlopScale.observe(this, observerForOnlyUpdates {
            setDrawerViewPagerSensitivity(drawerViewPager, defaultTouchSlop)
        })
    }

    /**
     * ドロワタブの遷移感度を下げる
     */
    private fun setDrawerViewPagerSensitivity(viewPager: ViewPager2, defaultTouchSlop: Int) {
        runCatching {
            val scale = 1 / (viewModel.drawerPagerTouchSlopScale.value ?: 1f)
            viewPager.touchSlop = (defaultTouchSlop * scale).toInt()
        }.onFailure {
            Log.e("viewPager2", Log.getStackTraceString(it))
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun setDrawerSwipeClosable(position: Int) {
        // ドロワ内のタブ切り替え操作と干渉するため
        // 一番端のタブを表示中以外はスワイプで閉じないようにする
        val drawerTabAdapter = binding.drawerViewPager.adapter!!
        val direction = resources.configuration.layoutDirection
        val actualGravity = Gravity.getAbsoluteGravity(viewModel.drawerGravity, direction)
        val closerEnabled = when (actualGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.LEFT -> position == drawerTabAdapter.itemCount - 1
            Gravity.RIGHT -> position == 0
            else -> true // not implemented gravity
        }
        binding.drawerLayout.setCloseSwipeEnabled(closerEnabled, binding.drawerArea)
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

        val toolbarBinding = toolbar.inflate(viewModel, this, binding.appbarLayout, true)
        setSupportActionBar(toolbarBinding.toolbar)

        viewModel.useBottomAppBar.observe(this) {
            binding.addressBarArea.post { initializeAddressBar(it) }
        }

        // クリック防止ビュー
        binding.clickGuard.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action != MotionEvent.ACTION_UP) return@setOnTouchListener false
            hideSoftInputMethod(binding.mainArea)
            return@setOnTouchListener true
        }
    }

    private fun initializeAddressBar(useBottomAppBar: Boolean) {
        val addressBar = R.id.address_bar_area
        val swipeLayout = R.id.swipe_layout
        val progressBar = R.id.progress_bar
        val parent = ConstraintSet.PARENT_ID
        val top = ConstraintSet.TOP
        val bottom = ConstraintSet.BOTTOM
        val (chain, progressSet) =
            if (useBottomAppBar) intArrayOf(swipeLayout, addressBar) to intArrayOf(bottom, top)
            else intArrayOf(addressBar, swipeLayout) to intArrayOf(top, bottom)

        with(ConstraintSet()) {
            clone(binding.mainArea)
            clear(progressBar, top)
            clear(progressBar, bottom)
            createVerticalChain(parent, top, parent, bottom, chain, null, 0)
            connect(progressBar, progressSet[0], addressBar, progressSet[1])
            applyTo(binding.mainArea)
        }
    }
}
