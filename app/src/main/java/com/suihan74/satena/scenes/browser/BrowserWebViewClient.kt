package com.suihan74.satena.scenes.browser

import android.graphics.Bitmap
import android.util.Log
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
        super.onPageStarted(view, url, favicon)
        viewModel.url.value = url
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        viewModel.title.value = view!!.title
        if (url != null) {
            viewModel.onPageFinished?.invoke(url)
        }
    }

    /** URLブロック対象のリソースを読み込まないようにする */
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (true != viewModel.repository.useUrlBlocking.value) {
            return super.shouldInterceptRequest(view, request)
        }

        val url = request?.url?.toString() ?: return null
        return if (!viewModel.repository.blockUrlsRegex.containsMatchIn(url)) {
            super.shouldInterceptRequest(view, request)
        }
        else {
            Log.i("abort", url)
            emptyResourceRequest
        }
    }

    /** すべてのリソースの読み込み時に呼ばれる */
    override fun onLoadResource(view: WebView?, url: String?) {
        url ?: return
        if (!viewModel.repository.blockUrlsRegex.containsMatchIn(url)) {
            super.onLoadResource(view, url)
        }
        else {
            Log.i("abort", url)
        }
    }
}
