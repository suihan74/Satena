package com.suihan74.satena.scenes.browser

import androidx.annotation.StringRes
import com.suihan74.satena.R

enum class WebViewTheme(
    val id: Int,
    @StringRes val textId: Int
) {
    /**
     * Satenaのテーマにあわせる
     *
     * LIGHT -> WebViewTheme.NORMAL
     * DARK -> WebViewTheme.DARK
     */
    AUTO(0,
        R.string.pref_browser_theme_auto
    ),

    /** とくに設定しない(メディアクエリなどを渡さずにサイトに任せる) */
    NORMAL(1,
        R.string.pref_browser_theme_normal
    ),

    /** 用意のあるサイトではダークテーマを使用する */
    DARK(2,
        R.string.pref_browser_theme_dark
    ),

    /** 強制的にダークテーマを使用する */
    FORCE_DARK(3,
        R.string.pref_browser_theme_force_dark
    )
}
