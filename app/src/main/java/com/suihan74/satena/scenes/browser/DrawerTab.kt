package com.suihan74.satena.scenes.browser

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.suihan74.satena.R
import com.suihan74.satena.scenes.browser.bookmarks.BookmarksFragment
import com.suihan74.satena.scenes.browser.history.HistoryFragment
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesFragment
import com.suihan74.satena.scenes.preferences.pages.PreferencesBrowserFragment

/** ドロワに表示するタブ */
enum class DrawerTab(
    val id: Int,
    @DrawableRes val iconId: Int,
    @StringRes val titleId: Int,
    val generator: ()->Fragment
) {
    BOOKMARKS(0,
        R.drawable.ic_baseline_bookmark,
        R.string.browser_drawer_title_bookmarks,
        { BookmarksFragment.createInstance() }
    ),

    FAVORITES(1,
        R.drawable.ic_star,
        R.string.browser_drawer_title_favorite_sites,
        { FavoriteSitesFragment.createInstance() }
    ),

    HISTORY(2,
        R.drawable.ic_category_history,
        R.string.browser_drawer_title_history,
        { HistoryFragment.createInstance() }
    ),

    SETTINGS(3,
        R.drawable.ic_baseline_settings,
        R.string.browser_drawer_title_preferences,
        { PreferencesBrowserFragment.createInstance() }
    );

    companion object {
        fun fromOrdinal(idx: Int) = values()[idx]
        fun fromId(id: Int) = values().first { it.id == id }
    }
}
