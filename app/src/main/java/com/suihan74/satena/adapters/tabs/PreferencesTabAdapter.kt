package com.suihan74.satena.adapters.tabs

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.suihan74.satena.R
import com.suihan74.satena.fragments.*

enum class PreferencesTabMode(
    val int : Int,
    val titleId : Int = 0,
    val iconId : Int = 0
) {
    // 環状スクロールできるように細工
    DUMMY_HEAD(0),

    INFORMATION(1,
        R.string.pref_title_information,
        R.id.preferences_tab_information),
    ACCOUNT(2,
        R.string.pref_title_account,
        R.id.preferences_tab_accounts),
    GENERALS(3,
        R.string.pref_title_generals,
        R.id.preferences_tab_generals),
    ENTRIES(4,
        R.string.pref_title_entries,
        R.id.preferences_tab_entries),
    BOOKMARKS(5,
        R.string.pref_title_bookmarks,
        R.id.preferences_tab_bookmarks),
    IGNORED_ENTRIES(6,
        R.string.pref_title_ignored_entries,
        R.id.preferences_tab_filters),
    IGNORED_USERS(7,
        R.string.pref_title_ignored_users,
        R.id.preferences_tab_ignored_users),

    DUMMY_TAIL(8);

    companion object {
        fun fromInt(i: Int) = values().firstOrNull { it.int == i } ?: INFORMATION
    }
}

class PreferencesTabAdapter(fm : FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getItem(position: Int): Fragment =
        when (PreferencesTabMode.fromInt(position)) {
            PreferencesTabMode.DUMMY_TAIL,
            PreferencesTabMode.INFORMATION -> PreferencesInformationFragment.createInstance()
            PreferencesTabMode.ACCOUNT -> PreferencesAccountsFragment.createInstance()
            PreferencesTabMode.GENERALS -> PreferencesGeneralsFragment.createInstance()
            PreferencesTabMode.ENTRIES -> PreferencesEntriesFragment.createInstance()
            PreferencesTabMode.BOOKMARKS -> PreferencesBookmarksFragment.createInstance()
            PreferencesTabMode.IGNORED_ENTRIES -> PreferencesIgnoredEntriesFragment.createInstance()
            PreferencesTabMode.IGNORED_USERS -> PreferencesIgnoredUsersFragment.createInstance()
            PreferencesTabMode.DUMMY_HEAD -> Fragment()
        }

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
