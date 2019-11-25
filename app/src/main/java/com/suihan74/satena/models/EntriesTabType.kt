package com.suihan74.satena.models

import com.suihan74.satena.R

enum class EntriesTabType(val tabPosition: Int, val textId: Int) {
    POPULAR(0, R.string.entries_tab_hot),
    RECENT(1, R.string.entries_tab_recent),
    MYBOOKMARKS(0, R.string.entries_tab_mybookmarks),
    READLATER(1, R.string.entries_tab_readlater);

    companion object {
        fun fromInt(i: Int) = values().getOrNull(i) ?: POPULAR
        fun fromCategory(category: Category, tabPosition: Int) = when (category) {
            Category.MyBookmarks -> fromInt(MYBOOKMARKS.ordinal + tabPosition)
            else -> fromInt(tabPosition)
        }
    }
}
