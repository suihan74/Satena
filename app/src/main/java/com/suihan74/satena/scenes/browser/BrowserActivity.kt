package com.suihan74.satena.scenes.browser

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ActivityBrowserBinding
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
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
            val initialUrl = intent.getStringExtra(EXTRA_URL)!!

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

        initializeWebView(webview)

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

    /** WebViewの設定 */
    private fun initializeWebView(wv: WebView) {
        wv.webViewClient = BrowserWebViewClient(viewModel)
        wv.webChromeClient = WebChromeClient()

        wv.setOnLongClickListener {
            val hitTestResult = wv.hitTestResult
            when (hitTestResult.type) {
                // 画像
                WebView.HitTestResult.IMAGE_TYPE -> {
                    Log.i("image", hitTestResult.extra ?: "")
                    true
                }

                // リンク
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    Log.i("link", hitTestResult.extra ?: "")
                    true
                }

                // 画像リンク
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    Log.i("imglink", hitTestResult.extra ?: "")
                    true
                }

                else -> false
            }
        }

        // jsのON/OFF
        viewModel.javascriptEnabled.observe(this, Observer {
            wv.settings.javaScriptEnabled = it
        })

        // UserAgentの設定
        viewModel.userAgent.observe(this, Observer {
            wv.settings.userAgentString = it
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.browser, menu)

        menu?.findItem(R.id.adblock)?.title =
            if (viewModel.useUrlBlocking.value == true) "AdBlock : ON"
            else "AdBlock : OFF"

        menu?.findItem(R.id.javascript)?.title =
            if (viewModel.javascriptEnabled.value == true) "JavaScript : ON"
            else "JavaScript : OFF"

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.bookmarks -> {
            val intent = Intent(this, BookmarksActivity::class.java).apply {
                putExtra(BookmarksActivity.EXTRA_ENTRY_URL, viewModel.url.value!!)
            }
            startActivity(intent)
            true
        }

        R.id.share -> {
            val intent = Intent().also {
                it.action = Intent.ACTION_SEND
                it.type = "text/plain"
                it.putExtra(Intent.EXTRA_TEXT, viewModel.url.value!!)
            }
            startActivity(intent)
            true
        }

        R.id.adblock -> {
            viewModel.useUrlBlocking.value = viewModel.useUrlBlocking.value != true
            item.title =
                if (viewModel.useUrlBlocking.value == true) "AdBlock : ON"
                else "AdBlock : OFF"
            webview.reload()
            true
        }

        R.id.javascript -> {
            viewModel.javascriptEnabled.value = viewModel.javascriptEnabled.value != true
            item.title =
                if (viewModel.javascriptEnabled.value == true) "JavaScript : ON"
                else "JavaScript : OFF"
            webview.reload()
            true
        }

        R.id.settings -> {
            // TODO:
            true
        }

        R.id.exit -> {
            finish()
            true
        }

        else -> false
    }
}
