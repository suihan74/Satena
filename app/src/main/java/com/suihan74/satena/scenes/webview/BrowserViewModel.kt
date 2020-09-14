package com.suihan74.satena.scenes.webview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

class BrowserViewModel(
    private val prefs: SafeSharedPreferences<PreferenceKey>,
    initialUrl: String
) : ViewModel() {
    /** テーマ */
    val themeId by lazy {
        if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppTheme_Dark
        else R.style.AppTheme_Light
    }

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
