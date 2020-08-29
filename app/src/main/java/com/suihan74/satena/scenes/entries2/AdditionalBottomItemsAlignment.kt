package com.suihan74.satena.scenes.entries2

import androidx.annotation.StringRes
import com.suihan74.satena.R

/** 画面によっては追加されるボトムバーボタンの追加位置 */
enum class AdditionalBottomItemsAlignment(
    val id: Int,
    @StringRes val textId: Int
) {
    DEFAULT(0,
        R.string.pref_additional_bottom_items_alignment_default
    ),

    LEFT(1,
        R.string.pref_additional_bottom_items_alignment_left
    ),

    RIGHT(2,
        R.string.pref_additional_bottom_items_alignment_right
    )

    ;

    companion object {
        fun fromInt(id: Int) = values().firstOrNull { it.id == id } ?: DEFAULT
    }
}
