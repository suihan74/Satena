package com.suihan74.satena.scenes.bookmarks

import androidx.annotation.StringRes
import com.suihan74.satena.R
import com.suihan74.satena.models.TextIdContainer

enum class TapTitleBarAction(
    val id: Int,
    @StringRes override val textId: Int
) : TextIdContainer {
    NOTHING(0, R.string.bookmark_title_action_nothing),

    SHOW_PAGE(1, R.string.bookmark_title_action_show_page),

    SHOW_PAGE_WITH_DIALOG(2, R.string.bookmark_title_action_show_page_dialog),

    SHARE(3, R.string.bookmark_title_action_share),

    SHOW_MENU(4, R.string.bookmark_title_action_show_menu);

    companion object {
        fun fromId(id : Int) = values().firstOrNull { id == it.id } ?: NOTHING
        fun fromOrdinal(pos : Int) = values().getOrElse(pos) { NOTHING }
    }
}
