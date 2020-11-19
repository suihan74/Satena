package com.suihan74.satena.models

import androidx.annotation.StringRes
import com.suihan74.satena.R

/**
 * ダイアログのテーマ設定
 */
enum class DialogThemeSetting(
    val id: Int,
    @StringRes val titleId: Int
) {
    /** アプリにあわせる */
    APP(0, R.string.pref_generals_dialog_theme_app),

    /** ライトテーマ */
    LIGHT(1, R.string.pref_generals_dialog_theme_light),

    /** ダークテーマ */
    DARK(2, R.string.pref_generals_dialog_theme_dark)
    ;

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id } ?: APP
        fun fromOrdinal(index: Int) = values().getOrElse(index) { APP }
    }

    val themeId : Int
        get() = when(this) {
            LIGHT -> R.style.AlertDialogStyle_Light
            DARK -> R.style.AlertDialogStyle_Dark
            APP -> 0
        }
}
