package com.suihan74.satena.models.readEntry

import com.suihan74.satena.R
import com.suihan74.satena.models.TextIdContainer

/**
 * どのような条件で既読エントリと判断されるか
 */
enum class ReadEntryCondition(
    val int : Int,
    override val textId : Int,
    /** 設定画面に表示して直接選択できる */
    private val visible : Boolean = true
) : TextIdContainer {
    /** 何も選択されていない */
    NONE(0b0000, R.string.entries_read_mark_condition_none, visible = false),

    /** ブクマ画面を開いた */
    BOOKMARKS_SHOWN(0b0001, R.string.entries_read_mark_condition_bookmarks_shown),

    /** Webページを開いた */
    PAGE_SHOWN(0b0010, R.string.entries_read_mark_condition_page_shown),

    /** ブクマ画面 or Webページ を開いた */
    BOOKMARKS_OR_PAGE_SHOWN(0b0011, R.string.entries_read_mark_condition_bookmarks_or_page_shown, visible = false)
    ;

    companion object {
        fun visibleValues() =
            values().filter { it.visible }

        /**
         * @throws NoSuchElementException
         */
        fun fromInt(i: Int) = values().first { it.int == i }
    }

    infix fun contains(other: ReadEntryCondition) : Boolean =
        this.int.and(other.int) > 0
}
