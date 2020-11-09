package com.suihan74.satena.scenes.browser

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.suihan74.utilities.IconFragmentPagerAdapter

class DrawerTabAdapter(
    fragmentManager: FragmentManager,
    behavior: Int = BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) : IconFragmentPagerAdapter(fragmentManager, behavior) {
    override fun getCount() : Int =
        DrawerTab.values().size

    override fun getItem(position: Int) : Fragment =
        DrawerTab.fromOrdinal(position).generator()

    @DrawableRes
    override fun getIconId(position: Int) : Int =
        DrawerTab.fromOrdinal(position).iconId

    @StringRes
    override fun getTitleId(position: Int): Int = 0

    @StringRes
    override fun getTooltipTextId(position: Int): Int =
        DrawerTab.fromOrdinal(position).titleId
}
