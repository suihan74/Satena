package com.suihan74.satena.models

import com.suihan74.satena.R

enum class TapEntryAction(val int: Int, val titleId: Int) {
    SHOW_COMMENTS(0, R.string.entry_action_show_comments),
    SHOW_PAGE(1, R.string.entry_action_show_page),
    SHOW_PAGE_IN_BROWSER(2, R.string.entry_action_show_page_in_browser),
    SHOW_MENU(3, R.string.entry_action_show_menu);

    companion object {
        fun fromInt(i : Int) : TapEntryAction = values().firstOrNull { it.int == i } ?: SHOW_COMMENTS
    }
}
