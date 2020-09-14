package com.suihan74.satena.scenes.webview

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class BrowserWebViewClient(
    private val viewModel: BrowserViewModel
) : WebViewClient() {

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
    }

    /** すべてのリソースの読み込み時に呼ばれる */
    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
    }
}
