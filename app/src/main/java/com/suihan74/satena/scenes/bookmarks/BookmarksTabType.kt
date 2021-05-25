package com.suihan74.satena.scenes.bookmarks

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.suihan74.satena.R
import com.suihan74.satena.models.TextIdContainer

enum class BookmarksTabType(
    @StringRes override val textId : Int,
    val createFragment : ()-> Fragment
) : TextIdContainer {
    POPULAR(R.string.bookmarks_tab_popular, { PopularBookmarksTabFragment.createInstance() }),
    RECENT(R.string.bookmarks_tab_recent, { RecentBookmarksTabFragment.createInstance(RECENT) }),
    ALL(R.string.bookmarks_tab_all, { RecentBookmarksTabFragment.createInstance(ALL) }),
    CUSTOM(R.string.bookmarks_tab_custom, { RecentBookmarksTabFragment.createInstance(CUSTOM) });

    companion object {
        fun fromOrdinal(i: Int) = values().getOrNull(i) ?: POPULAR
    }
}
