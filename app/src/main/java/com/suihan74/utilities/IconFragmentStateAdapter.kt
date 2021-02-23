package com.suihan74.utilities

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.suihan74.satena.R
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.extensions.onNot

/**
 * アイコンを表示するためのタブアダプタ
 */
abstract class IconFragmentStateAdapter : FragmentStateAdapter {

    constructor(activity: FragmentActivity) : super(activity) {
        fragmentManager = activity.supportFragmentManager
    }

    constructor(fragment: Fragment) : super(fragment) {
        fragmentManager = fragment.childFragmentManager
    }

    private val fragmentManager : FragmentManager

    // ------ //

    /**
     * アイコンID
     *
     * 表示しない場合0
     */
    @DrawableRes
    abstract fun getIconId(position: Int) : Int

    /**
     * タイトル文字列ID
     *
     * 表示しない場合0
     */
    @StringRes
    abstract fun getTitleId(position: Int) : Int

    /**
     * ツールチップの文字列ID
     *
     * 表示しない場合0
     */
    @StringRes
    abstract fun getTooltipTextId(position: Int) : Int

    /**
     * タイトル文字列を出力する
     */
    open fun getTitle(context: Context, position: Int, @StringRes textId: Int) : CharSequence {
        return context.getText(textId)
    }

    /**
     * ツールチップテキストを出力する
     */
    open fun getTooltipText(context: Context, position: Int, @StringRes textId: Int) : CharSequence {
        return context.getText(textId)
    }

    // ------ //

    fun findFragment(position: Int) : Fragment? = fragmentManager.findFragmentByTag("f$position")

    // ------ //

    fun setup(
        context: Context,
        tabLayout: TabLayout,
        viewPager: ViewPager2
    ) {
        viewPager.adapter = this
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // アイコン
            getIconId(position).onNot(0) { iconId ->
                tab.icon = ContextCompat.getDrawable(
                    context,
                    iconId
                )?.also { icon ->
                    val color = context.getThemeColor(
                        if (position == tabLayout.selectedTabPosition) R.attr.tabSelectedTextColor
                        else R.attr.tabTextColor
                    )
                    DrawableCompat.setColorFilter(icon, color)
                }
            }

            // タイトル
            getTitleId(position).onNot(0) { textId ->
                tab.text = getTitle(context, position, textId)
            }

            // ツールチップテキスト
            getTooltipTextId(position).onNot(0) { textId ->
                TooltipCompat.setTooltipText(
                    tab.view,
                    getTooltipText(context, position, textId)
                )
            }
        }.attach()
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
                findFragment(tab!!.position).alsoAs<ScrollableToTop> { f ->
                    f.scrollToTop()
                }
            }
        })
        notifyDataSetChanged()
    }
}
