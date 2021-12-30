package com.suihan74.satena.scenes.authentication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityHatenaAuthenticationBinding
import com.suihan74.satena.scenes.splash.SplashActivity
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.finish
import com.suihan74.utilities.extensions.toVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

class HatenaAuthenticationActivity : AppCompatActivity() {
    companion object {
        private const val BASE_URL = "https://www.hatena.ne.jp"
        private const val SIGN_IN_PAGE_URL = "$BASE_URL/login"

        const val EXTRA_FIRST_LAUNCHING = "EXTRA_FIRST_LAUNCHING"
    }

    // ------ //

    private val cookieManager = CookieManager.getInstance()

    private val rk: String?
        get() {
            val cookies = cookieManager.getCookie(BASE_URL)
            if (cookies.isNullOrBlank()) return null
            val regex = Regex("""rk=(.+);""")
            val matches = regex.find(cookies)
            return matches?.groupValues?.getOrNull(1)
        }

    private var finished = false

    private val loading = MutableLiveData<Boolean>()

    private lateinit var binding: ActivityHatenaAuthenticationBinding

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHatenaAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeWebView(binding.webView)

        loading.observe(this, {
            binding.clickGuard.visibility = it.toVisibility()
            binding.progressBar.visibility = it.toVisibility()
        })

        showToast(R.string.msg_hatena_sign_in_warning_on_creating)
    }

    private fun initializeWebView(webView: WebView) {
        cookieManager.acceptCookie()
        cookieManager.removeAllCookies {}
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        webView.settings.javaScriptEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.setInitialScale(1)
        webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        webView.webViewClient = WebViewClient()
        webView.loadUrl(SIGN_IN_PAGE_URL)
    }

    override fun finish() {
        binding.webView.finish()
        if (intent.getBooleanExtra(EXTRA_FIRST_LAUNCHING, false)) {
            val intent = Intent(this, SplashActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
        else {
            super.finish()
        }
    }

    // ------ //

    private fun onFinishAuthentication() {
        val rkStr = rk
        if (rkStr.isNullOrBlank()) {
            setResult(RESULT_CANCELED)
        }
        else {
            lifecycleScope.launch(Dispatchers.Main) {
                runCatching {
                    val accountLoader = SatenaApplication.instance.accountLoader
                    accountLoader.signInHatena(rkStr)
                }.onSuccess {
                    showToast(R.string.msg_hatena_sign_in_succeeded, HatenaClient.account!!.name)
                    setResult(RESULT_OK)
                }.onFailure {
                    showToast(R.string.msg_hatena_sign_in_failed)
                    setResult(RESULT_CANCELED)
                }
                finish()
            }
        }
    }

    private fun onError() {
        showToast(R.string.msg_hatena_sign_in_failed)
        setResult(RESULT_CANCELED)
        finish()
    }

    // ------ //

    inner class WebViewClient : android.webkit.WebViewClient() {
        private val emptyResourceRequest: WebResourceResponse =
            WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            // TODO: 以下の条件なしだとプロキシ利用などでリソース読み込み拒否した場合でも終了してしまうので、あとでなんとかする
            if (request?.url?.toString() == SIGN_IN_PAGE_URL) {
                onError()
            }
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            synchronized(this) {
                if (finished) return emptyResourceRequest
                if (request == null) return emptyResourceRequest
                if (request.url.toString() == SIGN_IN_PAGE_URL && request.method == "POST") {
                    lifecycleScope.launchWhenCreated {
                        runCatching { loading.value = true }
                    }
                }
                if (!finished && !rk.isNullOrBlank()) {
                    finished = true
                    onFinishAuthentication()
                    return emptyResourceRequest
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (url == SIGN_IN_PAGE_URL) {
                lifecycleScope.launchWhenCreated {
                    runCatching { loading.value = false }
                }
            }
        }
    }
}
