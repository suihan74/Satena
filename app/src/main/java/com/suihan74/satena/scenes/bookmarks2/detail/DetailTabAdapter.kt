package com.suihan74.satena.scenes.bookmarks2.detail

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager

class DetailTabAdapter(
    private val detailFragment: BookmarkDetailFragment
) : FragmentPagerAdapter(detailFragment.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    enum class TabType {
        STARS_TO_USER,
        STARS_FROM_USER
        ;

        companion object {
            fun fromInt(i: Int) = values()[i]
        }
    }

    override fun getCount() = TabType.values().size

    override fun getItem(position: Int) : Fragment =
        when (TabType.fromInt(position)) {
            TabType.STARS_TO_USER ->
                StarsToUserFragment.createInstance()

            TabType.STARS_FROM_USER ->
                StarsFromUserFragment.createInstance()
        }

    override fun getPageTitle(position: Int) =
        when (TabType.fromInt(position)) {
            TabType.STARS_TO_USER -> "★ to ${detailFragment.targetUser} (${detailFragment.starsCountToUser})"
            TabType.STARS_FROM_USER -> "★ from ${detailFragment.targetUser}"
        }

    fun findFragment(viewPager: ViewPager, position: Int) =
        instantiateItem(viewPager, position)
}
