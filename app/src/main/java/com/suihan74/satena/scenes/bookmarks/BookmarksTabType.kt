package com.suihan74.satena.scenes.bookmarks

import androidx.annotation.StringRes
import com.suihan74.satena.R

enum class BookmarksTabType(
    @StringRes val textId: Int
) {
    POPULAR(R.string.bookmarks_tab_popular),
    RECENT(R.string.bookmarks_tab_recent),
    ALL(R.string.bookmarks_tab_all),
    CUSTOM(R.string.bookmarks_tab_custom);

    companion object {
        fun fromOrdinal(i: Int) = values().getOrNull(i) ?: POPULAR
    }
}
