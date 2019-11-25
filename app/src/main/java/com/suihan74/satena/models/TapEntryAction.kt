package com.suihan74.satena.models

import com.suihan74.satena.R

enum class TapEntryAction(val titleId: Int) {
    SHOW_COMMENTS(R.string.entry_action_show_comments),
    SHOW_PAGE(R.string.entry_action_show_page),
    SHOW_PAGE_IN_BROWSER(R.string.entry_action_show_page_in_browser),
    SHOW_MENU(R.string.entry_action_show_menu);

    companion object {
        fun fromInt(i : Int) = values().getOrNull(i) ?: SHOW_COMMENTS
    }
}
