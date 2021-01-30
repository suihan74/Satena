package com.suihan74.satena.models

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.suihan74.satena.R
import com.suihan74.utilities.SafeSharedPreferences

/**
 * アプリテーマ
 */
enum class Theme(
    val id: Int,
    @StringRes val textId: Int,
    @StyleRes val themeId: Int,
    @StyleRes val dialogActivityThemeId: Int,
    @StyleRes val dialogFragmentStyleId: Int,
) {
    /** ライト */
    LIGHT(0,
        R.string.pref_generals_theme_light,
        R.style.AppTheme_Light,
        R.style.AppDialogTheme_Light,
        R.style.AlertDialogStyle_Light
    ),

    /** ダーク */
    DARK(1,
        R.string.pref_generals_theme_dark,
        R.style.AppTheme_Dark,
        R.style.AppDialogTheme_Dark,
        R.style.AlertDialogStyle_Dark
    ),

    /** 真っ黒 */
    EX_DARK(2,
        R.string.pref_generals_theme_ex_dark,
        R.style.AppTheme_ExDark,
        R.style.AppDialogTheme_ExDark,
        R.style.AlertDialogStyle_ExDark
    )
    ;

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id } ?: LIGHT

        @StyleRes
        fun themeId(prefs: SafeSharedPreferences<PreferenceKey>) : Int {
            val theme = fromId(prefs.getInt(PreferenceKey.THEME))
            return theme.themeId
        }

        @StyleRes
        fun dialogActivityThemeId(prefs: SafeSharedPreferences<PreferenceKey>) : Int {
            val theme = fromId(prefs.getInt(PreferenceKey.THEME))
            return theme.dialogActivityThemeId
        }

        @StyleRes
        fun dialogFragmentStyleId(prefs: SafeSharedPreferences<PreferenceKey>) : Int {
            val theme = fromId(prefs.getInt(PreferenceKey.THEME))
            return theme.dialogFragmentStyleId
        }
    }
}
