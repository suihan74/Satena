package com.suihan74.satena.scenes.browser

import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

class BrowserWebViewClient(
    private val activity: BrowserActivity,
    private val viewModel: BrowserViewModel
) : WebViewClient() {

    private val emptyResourceRequest : WebResourceResponse by lazy {
        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
    }

    /** ページに遷移するか否かを決定する */
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme

        return when (scheme) {
            "https", "http" ->
                super.shouldOverrideUrlLoading(view, request)

            "intent", "android-app" -> {
                try {
                    val intentScheme =
                        if (scheme == "intent") Intent.URI_INTENT_SCHEME
                        else Intent.URI_ANDROID_APP_SCHEME
                    val intent = Intent.parseUri(uri.toString(), intentScheme).also {
                        // 外部に公開していないアクティビティを開かないようにする加工を行い脆弱性を改善する
                        it.addCategory(Intent.CATEGORY_BROWSABLE)
                        it.component = null
                        it.selector?.let { selector ->
                            selector.addCategory(Intent.CATEGORY_BROWSABLE)
                            selector.component = null
                        }
                    }
                    activity.startActivity(intent)
                }
                catch (e: Throwable) {
                    Log.e("error", Log.getStackTraceString(e))
                }
                false
            }

            else -> false
        }
    }

    /** WebView内でのページ遷移をViewModelに伝える */
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        url ?: return
        super.onPageStarted(view, url, favicon)
        viewModel.onPageStarted(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null) {
            viewModel.onPageFinished(view, url)
        }
    }

    /** URLブロック対象のリソースを読み込まないようにする */
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        val blocked =
            viewModel.useUrlBlocking.value == true &&
            viewModel.browserRepo.blockUrlsRegex.containsMatchIn(url)

        val result =
            if (!blocked) super.shouldInterceptRequest(view, request)
            else emptyResourceRequest

        if (blocked) {
            viewModel.addResource(url, blocked = true)
        }

        return result
    }

    /** ページ中のリソースURLをすべて記録する */
    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
        if (url != null) {
            viewModel.addResource(url, blocked = false)
        }
    }
}
