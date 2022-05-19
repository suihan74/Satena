package com.suihan74.satena.scenes.browser

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class BrowserWebChromeClient(
    private val repo : BrowserRepository,
    viewModel : BrowserViewModel
) : WebChromeClient() {

    private val viewModelRef = WeakReference(viewModel)
    private val viewModel : BrowserViewModel?
        get() = viewModelRef.get()

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
        viewModel?.viewModelScope?.launch {
            repo.loadFavicon(icon)
        }
    }

    /**
     * ページタイトル取得
     */
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        viewModel?.onReceivedTitle(view, title)
    }
}
