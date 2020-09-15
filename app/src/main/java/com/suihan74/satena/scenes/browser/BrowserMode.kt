package com.suihan74.satena.scenes.browser

/** 使用する内部ブラウザ */
enum class BrowserMode(
    val id : Int
) {
    /** CustomTabsIntent */
    CUSTOM_TABS_INTENT(0),

    /** WebView */
    WEB_VIEW(1);

    companion object {
        fun fromInt(i: Int) = values().firstOrNull { it.id == i } ?: WEB_VIEW
    }
}
