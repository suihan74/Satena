package com.suihan74.satena.scenes.browser

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.utilities.extensions.estimatedHierarchy
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class BrowserWebChromeClient(
    private val browserRepo : BrowserRepository,
    private val historyRepo : HistoryRepository,
    private val favoriteSitesRepo : FavoriteSitesRepository,
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
        browserRepo.loadingProgress.value = newProgress
    }

    /**
     * favicon取得
     */
    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        val context = view?.context?.applicationContext
        val url = view?.url
        viewModel?.viewModelScope?.launch {
            runCatching {
                browserRepo.loadFavicon(icon)
                if (context != null && icon != null) {
                    historyRepo.saveFaviconCache(context, icon, url)
                    Uri.parse(url).estimatedHierarchy?.let {
                        favoriteSitesRepo.updateFavicon(it)
                    }
                }
            }
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
