package com.suihan74.satena.models

import android.content.Context
import com.suihan74.satena.R

enum class BookmarksTabType(val int: Int) {
    POPULAR(0),
    RECENT(1),
    // TODO: カスタムタブ
//    CUSTOM(2),
    ALL(2);

    companion object {
        fun fromInt(i: Int) : BookmarksTabType = values().firstOrNull { it.int == i } ?: POPULAR
    }

    fun toString(context : Context) : String = when(this) {
        POPULAR -> context.resources.getString(R.string.bookmarks_tab_popular)
        RECENT  -> context.resources.getString(R.string.bookmarks_tab_recent)
//        CUSTOM  -> context.resources.getString(R.string.bookmarks_tab_custom)
        ALL     -> context.resources.getString(R.string.bookmarks_tab_all)
    }
}
