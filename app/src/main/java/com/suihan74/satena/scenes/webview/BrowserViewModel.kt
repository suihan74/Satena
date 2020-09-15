package com.suihan74.satena.scenes.webview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

class BrowserViewModel(
    val repository: BrowserRepository,
    initialUrl: String
) : ViewModel() {
    /** テーマ */
    val themeId : Int
        get() = repository.themeId

    /** 表示中のページURL */
    val url by lazy {
        MutableLiveData(initialUrl)
    }

    /** 表示中のページタイトル */
    val title by lazy {
        MutableLiveData("")
    }

    /** JavaScriptを有効にする */
    val javascriptEnabled by lazy {
        MutableLiveData(true)
    }
}
