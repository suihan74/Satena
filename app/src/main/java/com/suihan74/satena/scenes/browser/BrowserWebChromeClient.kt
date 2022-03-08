package com.suihan74.satena.scenes.browser

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import java.lang.ref.WeakReference

class BrowserWebChromeClient(
    private val repo : BrowserRepository,
    viewModel : BrowserViewModel
) : WebChromeClient() {

    private val viewModelRef = WeakReference(viewModel)

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

    /**
     * ページタイトル取得
     */
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        viewModelRef.get()?.onReceivedTitle(view, title)
    }
}
