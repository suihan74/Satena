package com.suihan74.satena.scenes.entries2

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter

class EntriesTabAdapter(
    private val fragment: EntriesFragment
) : FragmentPagerAdapter(fragment.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int) : Fragment {
        return EntriesTabFragment.createInstance(fragment.viewModelKey, fragment.category, position)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        val context = fragment.context ?: return null
        val textId = fragment.getTabTitleId(position)
        return if (textId > 0) context.getText(textId) else ""
    }

    override fun getCount() = fragment.tabCount
}
