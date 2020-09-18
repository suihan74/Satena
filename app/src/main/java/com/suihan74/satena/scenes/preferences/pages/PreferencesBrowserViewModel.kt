package com.suihan74.satena.scenes.preferences.pages

import androidx.lifecycle.ViewModel
import com.suihan74.satena.scenes.browser.BrowserRepository

class PreferencesBrowserViewModel(
    repository: BrowserRepository
) : ViewModel() {

    val startPage by lazy {
        repository.startPage
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
}
