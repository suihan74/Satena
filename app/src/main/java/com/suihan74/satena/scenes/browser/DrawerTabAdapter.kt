package com.suihan74.satena.scenes.browser

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.utilities.*

abstract class IconFragmentPagerAdapter(
    fragmentManager: FragmentManager,
    behavior: Int = BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) : FragmentPagerAdapter(fragmentManager, behavior) {

    fun findFragment(viewPager: ViewPager, position: Int) =
        instantiateItem(viewPager, position) as? Fragment

    abstract fun getIconId(position: Int) : Int

    /** タブリストが変更されたときに呼ばれるリスナ */
    private var onDataSetChangedListener : Listener<Unit>? = null

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        onDataSetChangedListener?.invoke(Unit)
    }

    fun setup(
        context: Context,
        tabLayout: TabLayout,
        viewPager: ViewPager
    ) {
        onDataSetChangedListener = {
            repeat(count) { i ->
                tabLayout.getTabAt(i)?.icon = ContextCompat.getDrawable(
                    context,
                    getIconId(i)
                )?.also { icon ->
                    val color = context.getThemeColor(
                        if (i == tabLayout.selectedTabPosition) R.attr.tabSelectedTextColor
                        else R.attr.tabTextColor
                    )
                    DrawableCompat.setColorFilter(icon, color)
                }
            }
        }
        viewPager.adapter = this
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val icon = tab?.icon ?: return
                DrawableCompat.setColorFilter(icon, context.getThemeColor(R.attr.tabSelectedTextColor))
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val icon = tab?.icon ?: return
                DrawableCompat.setColorFilter(icon, context.getThemeColor(R.attr.tabTextColor))
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                findFragment(viewPager, tab!!.position).alsoAs<ScrollableToTop> { f ->
                    f.scrollToTop()
                }
            }
        })
        notifyDataSetChanged()
    }
}

///////////////////////////////////////////////////////////////////

class DrawerTabAdapter(
    fragmentManager: FragmentManager,
    behavior: Int = BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) : IconFragmentPagerAdapter(fragmentManager, behavior) {
    override fun getCount() : Int =
        DrawerTab.values().size

    override fun getItem(position: Int) : Fragment =
        DrawerTab.fromOrdinal(position).generator()

    override fun getIconId(position: Int) : Int =
        DrawerTab.fromOrdinal(position).iconId
}
