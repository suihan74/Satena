package com.suihan74.satena.models

import android.content.Context
import com.suihan74.satena.R

enum class BookmarksTabType(val int: Int) {
    POPULAR(0),
    RECENT(1),
    ALL(2),
    CUSTOM(3);

    companion object {
        fun fromInt(i: Int) : BookmarksTabType = values().firstOrNull { it.int == i } ?: POPULAR
    }

    fun toString(context : Context) : String = when(this) {
        POPULAR -> context.resources.getString(R.string.bookmarks_tab_popular)
        RECENT  -> context.resources.getString(R.string.bookmarks_tab_recent)
        ALL     -> context.resources.getString(R.string.bookmarks_tab_all)
        CUSTOM  -> context.resources.getString(R.string.bookmarks_tab_custom)
    }
}
