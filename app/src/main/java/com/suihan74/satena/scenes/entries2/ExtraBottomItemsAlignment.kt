package com.suihan74.satena.scenes.entries2

import androidx.annotation.StringRes
import com.suihan74.satena.R
import com.suihan74.satena.models.TextIdContainer

/** 画面によっては追加されるボトムバーボタンの追加位置 */
enum class ExtraBottomItemsAlignment(
    val id: Int,
    @StringRes override val textId: Int
) : TextIdContainer {
    DEFAULT(0,
        R.string.pref_extra_bottom_items_alignment_default
    ),

    LEFT(1,
        R.string.pref_extra_bottom_items_alignment_left
    ),

    RIGHT(2,
        R.string.pref_extra_bottom_items_alignment_right
    )

    ;

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id } ?: DEFAULT
        fun fromOrdinal(index: Int) = values().getOrElse(index) { DEFAULT }
    }
}
