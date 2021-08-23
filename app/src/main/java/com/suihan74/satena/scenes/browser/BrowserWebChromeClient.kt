package com.suihan74.satena.scenes.browser

import android.webkit.WebChromeClient
import android.webkit.WebView

class BrowserWebChromeClient(
    private val repo : BrowserRepository
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        repo.loadingProgress.value = newProgress
    }
}
