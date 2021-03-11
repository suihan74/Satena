package com.suihan74.satena.models

import androidx.annotation.StringRes
import com.suihan74.satena.R

enum class TapEntryAction(
    val id: Int,
    @StringRes override val textId: Int
) : TextIdContainer {
    NOTHING(4, R.string.entry_action_nothing),

    SHOW_COMMENTS(0, R.string.entry_action_show_comments),

    SHOW_PAGE(1, R.string.entry_action_show_page),

    SHOW_PAGE_IN_BROWSER(2, R.string.entry_action_show_page_in_browser),

    SHOW_MENU(3, R.string.entry_action_show_menu);

    companion object {
        fun fromId(id : Int) = values().firstOrNull { id == it.id } ?: SHOW_COMMENTS
        fun fromOrdinal(pos : Int) = values().getOrElse(pos) { SHOW_COMMENTS }
    }
}
