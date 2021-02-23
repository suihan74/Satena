package com.suihan74.satena.scenes.browser

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.suihan74.utilities.IconFragmentStateAdapter

class DrawerTabAdapter(activity: BrowserActivity) : IconFragmentStateAdapter(activity) {

    override fun getItemCount() : Int = DrawerTab.values().size

    override fun createFragment(position: Int) : Fragment =
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
