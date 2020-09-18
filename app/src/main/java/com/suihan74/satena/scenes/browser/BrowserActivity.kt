package com.suihan74.satena.scenes.browser

import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ActivityBrowserBinding
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.activity_browser.*
import kotlinx.coroutines.launch

class BrowserActivity : FragmentActivity() {
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

    private val FRAGMENT_BOOKMARK_POST = "FRAGMENT_BOOKMARK_POST"

    val viewModel : BrowserViewModel by lazy {
        provideViewModel(this) {
            val initialUrl = intent.getStringExtra(EXTRA_URL)

            val repository = BrowserRepository(
                HatenaClient,
                AccountLoader(this, HatenaClient, MastodonClientHolder),
                SafeSharedPreferences.create<PreferenceKey>(this),
                SafeSharedPreferences.create<BrowserSettingsKey>(this)
            )

            lifecycleScope.launch {
                repository.initialize()
            }

            BrowserViewModel(repository, initialUrl)
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

        setActionBar(toolbar)

        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                hideSoftInputMethod(main_area)
            }
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        viewModel.initializeWebView(webview, this)

        // IMEの決定ボタンでページ遷移する
        address_edit_text.setOnEditorActionListener { _, action, _ ->
            when (action) {
                EditorInfo.IME_ACTION_GO ->
                    viewModel.goAddress().whenTrue {
                        hideSoftInputMethod(main_area)
                    }

                else -> false
            }
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

        // 投稿エリアを作成
        val bookmarkPostFragment = BookmarkPostFragment.createInstance()
        supportFragmentManager.beginTransaction()
            .add(R.id.bookmark_post_area, bookmarkPostFragment, FRAGMENT_BOOKMARK_POST)
            .commitAllowingStateLoss()
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
            viewModel.bindOptionsMenu(this, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return viewModel.onOptionsItemSelected(item, this)
    }
}
