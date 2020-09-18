package com.suihan74.satena.scenes.browser

import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.suihan74.satena.R
import com.suihan74.satena.scenes.browser.bookmarks.BookmarksFragment
import com.suihan74.satena.scenes.preferences.pages.PreferencesBrowserFragment

/** ドロワに表示するタブ */
enum class DrawerTab(
    val id: Int,
    @DrawableRes val iconId: Int,
    val generator: ()->Fragment
) {
    BOOKMARKS(0,
        R.drawable.ic_baseline_bookmark,
        { BookmarksFragment.createInstance() }
    ),

    FAVORITES(1,
        R.drawable.ic_star,
        { Fragment() }
    ),

    HISTORY(2,
        R.drawable.ic_category_history,
        { Fragment() }
    ),

    SETTINGS(3,
        R.drawable.ic_baseline_settings,
        { PreferencesBrowserFragment.createInstance() }
    );

    companion object {
        fun fromOrdinal(idx: Int) = values()[idx]
        fun fromId(id: Int) = values().first { it.id == id }
    }
}
