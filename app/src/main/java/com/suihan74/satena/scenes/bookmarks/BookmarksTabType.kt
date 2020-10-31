package com.suihan74.satena.scenes.bookmarks

import androidx.annotation.StringRes
import com.suihan74.satena.R

enum class BookmarksTabType(
    @StringRes val textId : Int,
    val createFragment : ()->BookmarksTabFragment
) {
    POPULAR(R.string.bookmarks_tab_popular, { PopularBookmarksTabFragment.createInstance() }),
    RECENT(R.string.bookmarks_tab_recent, { RecentBookmarksTabFragment.createInstance(RECENT) }),
    ALL(R.string.bookmarks_tab_all, { RecentBookmarksTabFragment.createInstance(ALL) }),
    CUSTOM(R.string.bookmarks_tab_custom, { RecentBookmarksTabFragment.createInstance(CUSTOM) });

    companion object {
        fun fromOrdinal(i: Int) = values().getOrNull(i) ?: POPULAR
    }
}
