package com.suihan74.satena.scenes.bookmarks2

import com.suihan74.satena.R

enum class BookmarksTabType(val textId: Int) {
    POPULAR(R.string.bookmarks_tab_popular),
    RECENT(R.string.bookmarks_tab_recent),
    ALL(R.string.bookmarks_tab_all),
    CUSTOM(R.string.bookmarks_tab_custom);

    companion object {
        fun fromInt(i: Int) = values().getOrNull(i) ?: POPULAR
    }
}
