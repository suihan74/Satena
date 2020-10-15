package com.suihan74.satena.scenes.preferences.browser

import androidx.lifecycle.ViewModel
import com.suihan74.satena.scenes.browser.BrowserRepository

class UrlBlockingViewModel(
    val repository : BrowserRepository
) : ViewModel() {

    val blockUrls by lazy {
        repository.blockUrls
    }

}
