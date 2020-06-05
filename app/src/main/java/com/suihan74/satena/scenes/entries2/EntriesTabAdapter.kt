package com.suihan74.satena.scenes.entries2

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.suihan74.utilities.map

class EntriesTabAdapter(
    private val container: ViewGroup,
    private val fragment: EntriesFragment
) : FragmentPagerAdapter(fragment.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int) : Fragment =
        EntriesTabFragment.createInstance(fragment.viewModelKey, fragment.category, position)

    override fun getPageTitle(position: Int): CharSequence? =
        fragment.getTabTitle(position)

    override fun getCount() = fragment.tabCount

    /** すべてのタブのリストを再構成する */
    fun refreshLists() {
        map<EntriesTabFragment>(container) {
            it.reload()
        }
    }
