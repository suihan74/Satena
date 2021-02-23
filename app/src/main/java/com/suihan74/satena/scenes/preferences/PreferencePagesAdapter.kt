package com.suihan74.satena.scenes.preferences

import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.suihan74.satena.R
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesFragment
import com.suihan74.satena.scenes.preferences.pages.*

enum class PreferencesTabMode(
    val int : Int,
    @StringRes val titleId : Int = 0,
    @LayoutRes val iconId : Int = 0,
    val fragmentGenerator : () -> Fragment = { Fragment() }
) {
    // 環状スクロールできるように細工
    DUMMY_HEAD(0),

    INFORMATION(1,
        R.string.pref_title_information,
        R.id.preferences_tab_information,
        { InformationFragment() }
    ),

    ACCOUNT(2,
        R.string.pref_title_account,
        R.id.preferences_tab_accounts,
        { AccountFragment() }
    ),

    GENERALS(3,
        R.string.pref_title_generals,
        R.id.preferences_tab_generals,
        { GeneralFragment() }
    ),

    ENTRIES(4,
        R.string.pref_title_entries,
        R.id.preferences_tab_entries,
        { EntryFragment() }
    ),

    BOOKMARKS(5,
        R.string.pref_title_bookmarks,
        R.id.preferences_tab_bookmarks,
        { BookmarkFragment() }
    ),

    BROWSER(6,
        R.string.pref_title_browser,
        R.id.preferences_tab_browser,
        { BrowserFragment() }
    ),

    FAVORITE_SITES(7,
        R.string.category_favorite_sites,
        R.id.preferences_tab_favorite_sites,
        { FavoriteSitesFragment.createInstance() }),

    IGNORED_ENTRIES(8,
        R.string.pref_title_ignored_entries,
        R.id.preferences_tab_filters,
        { PreferencesIgnoredEntriesFragment.createInstance() }),

    IGNORED_USERS(9,
        R.string.pref_title_ignored_users,
        R.id.preferences_tab_ignored_users,
        { PreferencesIgnoredUsersFragment.createInstance() }),

    USER_TAGS(10,
        R.string.pref_title_user_tags,
        R.id.preferences_tab_user_tags,
        { PreferencesUserTagsFragment.createInstance() }),

    DUMMY_TAIL(11);

    companion object {
        fun fromId(i: Int) = values().firstOrNull { it.int == i } ?: INFORMATION
    }
}

// ------ //

class PreferencesTabAdapter(private val activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun createFragment(position: Int): Fragment =
        PreferencesTabMode.fromId(position).fragmentGenerator.invoke()

    fun getIconId(fixedPosition: Int) =
        PreferencesTabMode.fromId(fixedPosition + 1).iconId

    fun getIndexFromIconId(iconId : Int) =
        PreferencesTabMode.values().firstOrNull { iconId == getIconId(it.int - 1) }?.int ?: 0

    override fun getItemCount(): Int = PreferencesTabMode.values().size
    fun getActualCount() = itemCount - 2

    fun findFragment(position: Int) : Fragment? = activity.supportFragmentManager.findFragmentByTag("f$position")
}
