package com.suihan74.satena.scenes.webview

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
                webView.loadUrl(url)
            }
        }

        @BindingAdapter("title")
        @JvmStatic
        fun setToolbarTitle(toolbar: MarqueeToolbar, title: String) {
            if (toolbar.title != title) {
                toolbar.title = title
            }
        }
    }

    private val viewModel : BrowserViewModel by lazy {
        provideViewModel(this) {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
            val initialUrl = intent.getStringExtra(EXTRA_URL)!!

            val repository = BrowserRepository(
                HatenaClient,
                AccountLoader(this, HatenaClient, MastodonClientHolder),
                SafeSharedPreferences.create(this)
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

        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                hideSoftInputMethod()
            }
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        webview.let { wv ->
            wv.webViewClient = BrowserWebViewClient(viewModel)
            viewModel.javascriptEnabled.observe(this, Observer {
                wv.settings.javaScriptEnabled = it
            })
        }
    }

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
}
