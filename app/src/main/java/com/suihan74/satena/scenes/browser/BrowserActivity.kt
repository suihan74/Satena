package com.suihan74.satena.scenes.browser

import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.tabs.TabLayout
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityBrowserBinding
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.FavoriteSitesKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.browser.favorites.FavoriteSitesRepository
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.extensions.hideSoftInputMethod
import com.suihan74.utilities.provideViewModel
import kotlinx.android.synthetic.main.activity_browser.*

class BrowserActivity : AppCompatActivity() {
    companion object {
        /** 最初に開くページのURL */
        const val EXTRA_URL = "BrowserActivity.EXTRA_URL"

        @BindingAdapter("url")
        @JvmStatic
        fun loadUrl(webView: WebView, url: String) {
            if (webView.url != url) {
                webView.stopLoading()
                webView.loadUrl(url)
            }
        }
    }

    val viewModel : BrowserViewModel by lazy {
        provideViewModel(this) {
            val initialUrl = intent.getStringExtra(EXTRA_URL)

            val browserRepo = BrowserRepository(
                HatenaClient,
                AccountLoader(this, HatenaClient, MastodonClientHolder),
                SafeSharedPreferences.create<PreferenceKey>(this),
                SafeSharedPreferences.create<BrowserSettingsKey>(this)
            )

            val favoriteSitesRepo = FavoriteSitesRepository(
                SafeSharedPreferences.create<FavoriteSitesKey>(this)
            )

            val historyRepo = HistoryRepository(
                SatenaApplication.instance.browserDao
            )

            BrowserViewModel(browserRepo, favoriteSitesRepo, historyRepo, initialUrl)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(viewModel.themeId)

        DataBindingUtil.setContentView<ActivityBrowserBinding>(
            this,
            R.layout.activity_browser
        ).apply {
            vm = viewModel
            lifecycleOwner = this@BrowserActivity
        }

        // ドロワーを開いたときにIMEを閉じる
        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                hideSoftInputMethod(main_area)
            }
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        val drawerTabAdapter = DrawerTabAdapter(supportFragmentManager)
        drawerTabAdapter.setup(this, drawer_tab_layout, drawer_view_pager)
        drawer_tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // ドロワー内のタブ切り替え操作と干渉するため
                // 一番左のタブを表示中以外はスワイプで閉じないようにする
                drawer_layout.setCloseSwipeEnabled(tab?.position == 0, drawer_area)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // WebViewの設定
        viewModel.initializeWebView(webview, this)

        // ツールバーをセット
        val toolbar = BrowserToolbar(this)
        viewModel.useBottomAppBar.observe(this) {
            // もう一方の方をクリアしておく
            val another =
                if (it) appbar_layout
                else bottom_app_bar
            another.removeAllViews()

            val appBar =
                if (it) bottom_app_bar
                else appbar_layout

            val toolbarBinding = toolbar.inflate(viewModel, this, appBar, true)
            setSupportActionBar(toolbarBinding.toolbar)
        }

        // スワイプしてページを更新する
        swipe_layout.let {
            it.setProgressBackgroundColorSchemeColor(getThemeColor(R.attr.swipeRefreshBackground))
            it.setColorSchemeColors(getThemeColor(R.attr.colorPrimary))
            it.setOnRefreshListener {
                viewModel.setOnPageFinishedListener {
                    swipe_layout.isRefreshing = false
                    viewModel.setOnPageFinishedListener(null)
                }
                webview.reload()
            }
        }
    }

    /** 「戻る」ボタンでブラウザの履歴を戻る */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when {
        keyCode != KeyEvent.KEYCODE_BACK -> super.onKeyDown(keyCode, event)

        drawer_layout.isDrawerOpen(drawer_area) -> {
            drawer_layout.closeDrawer(drawer_area)
            true
        }

        webview.canGoBack() -> {
            webview.goBack()
            true
        }

        else -> super.onKeyDown(keyCode, event)
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

    /** ドロワを開いて設定タブを表示する */
    fun showPreferencesFragment() {
        drawer_view_pager.currentItem = DrawerTab.SETTINGS.ordinal
        drawer_layout.openDrawer(drawer_area)
    }

    /**
     * ドロワを閉じる
     *
     * BrowserActivityに依存するフラグメント側から閉じるために使用
     */
    fun closeDrawer() {
        drawer_layout.closeDrawer(drawer_area)
    }

    /**
     * URLを開く
     *
     * BrowserActivityに依存するフラグメント側からページ遷移するために使用
     */
    fun openUrl(url: String) {
        viewModel.goAddress(url)
        closeDrawer()
    }
}
