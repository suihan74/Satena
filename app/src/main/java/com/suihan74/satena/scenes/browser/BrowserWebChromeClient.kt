package com.suihan74.satena.scenes.browser

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView

class BrowserWebChromeClient(
    private val repo : BrowserRepository
) : WebChromeClient() {
    /**
     * ページ読み込み進捗
     */
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        repo.loadingProgress.value = newProgress
    }

    /**
     * favicon取得
     */
    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        if (repo.faviconLoading.value == true) {
            repo.faviconLoading.value = false
            repo.faviconBitmap.value = icon
        }
    }
}
