package com.suihan74.satena.models

import androidx.annotation.StringRes
import com.suihan74.satena.R

/**
 * ダイアログのテーマ設定
 */
enum class DialogThemeSetting(
    val id: Int,
    @StringRes override val textId: Int
) : TextIdContainer {
    /** アプリにあわせる */
    APP(0, R.string.pref_generals_dialog_theme_app),

    /** ライトテーマ */
    LIGHT(1, R.string.pref_generals_dialog_theme_light),

    /** ダークテーマ */
    DARK(2, R.string.pref_generals_dialog_theme_dark),

    /** 真っ黒 */
    EX_DARK(3, R.string.pref_generals_theme_ex_dark)
    ;

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id } ?: APP
        fun fromOrdinal(index: Int) = values().getOrElse(index) { APP }
    }

    val themeId : Int
        get() = when(this) {
            LIGHT -> Theme.LIGHT.dialogFragmentStyleId
            DARK -> Theme.DARK.dialogFragmentStyleId
            EX_DARK -> Theme.EX_DARK.dialogFragmentStyleId
            APP -> 0
        }
}
