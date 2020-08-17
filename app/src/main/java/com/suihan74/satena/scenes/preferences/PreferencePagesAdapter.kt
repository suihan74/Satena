package com.suihan74.satena.scenes.preferences

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.suihan74.satena.R
import com.suihan74.satena.scenes.preferences.pages.*

enum class PreferencesTabMode(
    val int : Int,
    val titleId : Int = 0,
    val iconId : Int = 0,
    val fragmentGenerator : () -> Fragment = { Fragment() }
) {
    // 環状スクロールできるように細工
    DUMMY_HEAD(0),

    INFORMATION(1,
        R.string.pref_title_information,
        R.id.preferences_tab_information,
        { PreferencesInformationFragment.createInstance() }),

    ACCOUNT(2,
        R.string.pref_title_account,
        R.id.preferences_tab_accounts,
        { PreferencesAccountsFragment.createInstance() }),

    GENERALS(3,
        R.string.pref_title_generals,
        R.id.preferences_tab_generals,
        { PreferencesGeneralsFragment.createInstance() }),

    ENTRIES(4,
        R.string.pref_title_entries,
        R.id.preferences_tab_entries,
        { PreferencesEntriesFragment.createInstance() }),

    BOOKMARKS(5,
        R.string.pref_title_bookmarks,
        R.id.preferences_tab_bookmarks,
        { PreferencesBookmarksFragment.createInstance() }),

    IGNORED_ENTRIES(6,
        R.string.pref_title_ignored_entries,
        R.id.preferences_tab_filters,
        { PreferencesIgnoredEntriesFragment.createInstance() }),

    IGNORED_USERS(7,
        R.string.pref_title_ignored_users,
        R.id.preferences_tab_ignored_users,
        { PreferencesIgnoredUsersFragment.createInstance() }),

    USER_TAGS(8,
        R.string.pref_title_user_tags,
        R.id.preferences_tab_user_tags,
        { PreferencesUserTagsFragment.createInstance() }),

    DUMMY_TAIL(9);

    companion object {
        fun fromInt(i: Int) = values().firstOrNull { it.int == i } ?: INFORMATION
    }
}

class PreferencesTabAdapter(fm : FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment =
        PreferencesTabMode.fromInt(position).fragmentGenerator.invoke()

    fun getPageTitleId(fixedPosition: Int) =
        PreferencesTabMode.fromInt(fixedPosition + 1).titleId

    fun getIconId(fixedPosition: Int) =
        PreferencesTabMode.fromInt(fixedPosition + 1).iconId

    fun getIndexFromIconId(iconId : Int) =
        PreferencesTabMode.values().firstOrNull { iconId == getIconId(it.int - 1) }?.int ?: 0

    override fun getCount() = PreferencesTabMode.values().size
    fun getActualCount() = count - 2

    fun findFragment(viewPager: ViewPager, position: Int) =
        instantiateItem(viewPager, position) as Fragment
}
