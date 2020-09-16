package com.suihan74.satena.scenes.browser

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

class BrowserWebViewClient(
    private val viewModel: BrowserViewModel
) : WebViewClient() {

    private val emptyResourceRequest : WebResourceResponse by lazy {
        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()));
    }

    /** ページに遷移するか否かを決定する */
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return super.shouldOverrideUrlLoading(view, request)
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
            viewModel.repository.blockUrlsRegex.containsMatchIn(url)

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
