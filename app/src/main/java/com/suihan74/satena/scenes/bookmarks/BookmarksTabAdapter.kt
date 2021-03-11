package com.suihan74.satena.scenes.bookmarks

import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class BookmarksTabAdapter(val activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private fun getTabType(position: Int) : BookmarksTabType =
        BookmarksTabType.fromOrdinal(position)

    fun getTitleId(position: Int) : Int =
        getTabType(position).textId

    override fun createFragment(position: Int): Fragment =
        getTabType(position).createFragment()

    override fun getItemCount(): Int = BookmarksTabType.values().size

    /**
     * 現在表示中のフラグメントを取得する
     */
    fun currentFragment(viewPager: ViewPager2) : Fragment? {
        val idx = viewPager.currentItem
        return activity.supportFragmentManager.findFragmentByTag("f$idx")
    }

    // ------ //

    object BindingAdapters {
        /**
         * ```ViewPager```の選択位置を設定する
         */
        @JvmStatic
        @BindingAdapter("currentTab")
        fun setCurrentTab(viewPager: ViewPager2, tabType: BookmarksTabType?) {
            if (tabType != null && viewPager.currentItem != tabType.ordinal) {
                viewPager.currentItem = tabType.ordinal
            }
        }
    }
}
