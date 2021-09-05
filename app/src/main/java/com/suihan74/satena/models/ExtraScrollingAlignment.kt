package com.suihan74.satena.models

import androidx.annotation.StringRes
import com.suihan74.satena.R

/**
 * エクストラスクロール機能のツマミの配置
 */
enum class ExtraScrollingAlignment(
    val id: Int,
    @StringRes override val textId: Int
) : TextIdContainer {

    /** エクストラスクロールを使用しない */
    NONE(0, R.string.pref_extra_scroll_align_none),

    /** 画面左側にツマミを表示する */
    LEFT(1, R.string.pref_extra_scroll_align_left),

    /** 画面右側にツマミを表示する */
    RIGHT(2, R.string.pref_extra_scroll_align_right)
    ;

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id } ?: NONE
    }
}
