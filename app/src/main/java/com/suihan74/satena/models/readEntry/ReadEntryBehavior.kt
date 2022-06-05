package com.suihan74.satena.models.readEntry

import com.suihan74.satena.R
import com.suihan74.satena.models.TextIdContainer

/** 既読エントリの振舞い */
enum class ReadEntryBehavior(
    val int: Int,
    override val textId: Int
) : TextIdContainer {

    /** 何もしない */
    NONE(0, R.string.read_entry_behavior_none),

    /** 既読マークを表示 */
    DISPLAY_READ_MARK(1, R.string.read_entry_behavior_display_read_mark),

    /** エントリを非表示 */
    HIDE_ENTRY(2, R.string.read_entry_behavior_hide_entry)
    ;

    companion object {
        fun fromInt(int: Int) = values().first { it.int == int }
    }
}
