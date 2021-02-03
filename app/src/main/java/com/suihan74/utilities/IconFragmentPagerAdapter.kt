package com.suihan74.utilities

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.extensions.onNot

/**
 * アイコンを表示するためのタブアダプタ
 */
abstract class IconFragmentPagerAdapter(
    fragmentManager: FragmentManager,
    behavior: Int = BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) : FragmentPagerAdapter(fragmentManager, behavior) {

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

    /** タブリストが変更されたときに呼ばれるリスナ */
    private var onDataSetChangedListener : Listener<Unit>? = null

    // ------ //

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        onDataSetChangedListener?.invoke(Unit)
    }

    fun findFragment(viewPager: ViewPager, position: Int) =
        instantiateItem(viewPager, position) as? Fragment

    fun setup(
        context: Context,
        tabLayout: TabLayout,
        viewPager: ViewPager
    ) {
        onDataSetChangedListener = {
            repeat(count) { i ->
                val tab = tabLayout.getTabAt(i) ?: return@repeat

                // アイコン
                getIconId(i).onNot(0) { iconId ->
                    tab.icon = ContextCompat.getDrawable(
                        context,
                        iconId
                    )?.also { icon ->
                        val color = context.getThemeColor(
                            if (i == tabLayout.selectedTabPosition) R.attr.tabSelectedTextColor
                            else R.attr.tabTextColor
                        )
                        DrawableCompat.setColorFilter(icon, color)
                    }
                }

                // タイトル
                getTitleId(i).onNot(0) { textId ->
                    tab.text = getTitle(context, i, textId)
                }

                // ツールチップテキスト
                getTooltipTextId(i).onNot(0) { textId ->
                    TooltipCompat.setTooltipText(
                        tab.view,
                        getTooltipText(context, i, textId)
                    )
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
