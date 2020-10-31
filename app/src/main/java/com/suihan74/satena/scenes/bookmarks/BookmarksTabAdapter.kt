package com.suihan74.satena.scenes.bookmarks

import android.content.Context
import androidx.databinding.BindingAdapter
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager

class BookmarksTabAdapter(
    val context: Context,
    fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private fun getTabType(position: Int) : BookmarksTabType =
        BookmarksTabType.fromOrdinal(position)

    override fun getItem(position: Int) =
        getTabType(position).createFragment()

    override fun getPageTitle(position: Int): CharSequence? =
        context.getString(getTabType(position).textId)

    override fun getCount() = BookmarksTabType.values().size

    // ------ //

    object BindingAdapters {
        /**
         * ```ViewPager```の選択位置を設定する
         */
        @JvmStatic
        @BindingAdapter("currentTab")
        fun setCurrentTab(viewPager: ViewPager, tabType: BookmarksTabType?) {
            if (tabType != null && viewPager.currentItem != tabType.ordinal) {
                viewPager.currentItem = tabType.ordinal
            }
        }
    }
}
