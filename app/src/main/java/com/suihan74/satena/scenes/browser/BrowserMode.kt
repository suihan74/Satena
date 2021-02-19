package com.suihan74.satena.scenes.browser

import androidx.annotation.StringRes
import com.suihan74.satena.R
import com.suihan74.satena.models.TextIdContainer

/** 使用する内部ブラウザ */
enum class BrowserMode(
    val id : Int,
    @StringRes override val textId: Int
) : TextIdContainer {
    /** CustomTabsIntent */
    CUSTOM_TABS_INTENT(0,
        R.string.pref_browser_mode_chrome_custom_tab
    ),

    /** WebView */
    WEB_VIEW(1,
        R.string.pref_browser_mode_web_view
    );

    companion object {
        fun fromId(i: Int) = values().firstOrNull { it.id == i } ?: WEB_VIEW
        fun fromOrdinal(i: Int) = values().getOrElse(i) { WEB_VIEW }
    }
}
