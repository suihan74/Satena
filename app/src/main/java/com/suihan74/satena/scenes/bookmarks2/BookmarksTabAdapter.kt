package com.suihan74.satena.scenes.bookmarks2

import androidx.fragment.app.FragmentPagerAdapter

open class BookmarksTabAdapter (
    private val bookmarksFragment: BookmarksFragment
) : FragmentPagerAdapter(bookmarksFragment.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int) =
        BookmarksTabFragment.createInstance(BookmarksTabType.fromOrdinal(position))

    override fun getPageTitle(position: Int): CharSequence? =
        bookmarksFragment.getString(BookmarksTabType.fromOrdinal(position).textId)

    override fun getCount() = BookmarksTabType.values().size
}
