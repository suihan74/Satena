package com.suihan74.satena.scenes.bookmarks2

import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager

open class BookmarksTabAdapter (
    private val bookmarksFragment: BookmarksFragment
) : FragmentPagerAdapter(bookmarksFragment.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int) =
        BookmarksTabFragment.createInstance(BookmarksTabType.fromInt(position))

    override fun getPageTitle(position: Int): CharSequence? =
        bookmarksFragment.getString(BookmarksTabType.fromInt(position).textId)

    override fun getCount() = BookmarksTabType.values().size

    fun findFragment(viewPager: ViewPager, position: Int) =
        instantiateItem(viewPager, position) as? BookmarksTabFragment
}
