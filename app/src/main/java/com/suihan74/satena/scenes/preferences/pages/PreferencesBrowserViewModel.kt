package com.suihan74.satena.scenes.preferences.pages

import androidx.lifecycle.ViewModel
import com.suihan74.satena.scenes.browser.BrowserRepository

class PreferencesBrowserViewModel(
    repository: BrowserRepository
) : ViewModel() {

    /** 設定アクティビティで開かれているかどうか */
    var isPreferencesActivity : Boolean = false

    val browserMode by lazy {
        repository.browserMode
    }

    val startPage by lazy {
        repository.startPage
    }

    val secretModeEnabled by lazy {
        repository.privateBrowsingEnabled
    }

    val javascriptEnabled by lazy {
        repository.javascriptEnabled
    }

    val userAgent by lazy {
        repository.userAgent
    }

    val useUrlBlock by lazy {
        repository.useUrlBlocking
    }

    val useBottomAppBar by lazy {
        repository.useBottomAppBar
    }

    val webViewTheme by lazy {
        repository.webViewTheme
    }

    val isForceDarkSupported by lazy {
        repository.isForceDarkSupported
    }

    val isForceDarkStrategySupported by lazy {
        repository.isForceDarkStrategySupported
    }
}
