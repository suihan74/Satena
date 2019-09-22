package com.suihan74.satena.adapters.tabs

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.fragments.BookmarksTabFragment
import com.suihan74.satena.models.BookmarksTabType

open class BookmarksTabAdapter(
    private val bookmarksActivity: BookmarksActivity,
    private val viewPager: ViewPager
) : FragmentPagerAdapter(bookmarksActivity.bookmarksFragment!!.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return BookmarksTabFragment.createInstance(this, BookmarksTabType.fromInt(position))
    }

    override fun getPageTitle(position: Int): CharSequence? = BookmarksTabType.fromInt(position).toString(bookmarksActivity.bookmarksFragment!!.context!!)

    override fun getCount() = BookmarksTabType.values().size

    fun findFragment(position: Int) = instantiateItem(viewPager, position) as BookmarksTabFragment

    fun update() {
        for (i in 0 until count) {
            val fragment = findFragment(i)
            fragment.update()
        }
    }

    fun removeBookmark(bookmark: Bookmark) {
        for (i in 0 until count) {
            val fragment = findFragment(i)
            fragment.removeBookmark(bookmark)
        }
    }

    open fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    }
}
