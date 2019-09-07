package com.suihan74.satena.adapters.tabs

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.RecyclerView
import com.suihan74.satena.fragments.BookmarksFragment
import com.suihan74.satena.fragments.BookmarksTabFragment
import com.suihan74.satena.models.BookmarksTabType

open class BookmarksTabAdapter(
    private val bookmarksFragment : BookmarksFragment,
    private val viewPager: ViewPager
) : FragmentPagerAdapter(bookmarksFragment.childFragmentManager) {

    override fun getItem(position: Int): Fragment {
        return BookmarksTabFragment.createInstance(bookmarksFragment, this, BookmarksTabType.fromInt(position))
    }

    override fun getPageTitle(position: Int): CharSequence? = BookmarksTabType.fromInt(position).toString(bookmarksFragment.context!!)

    override fun getCount() = BookmarksTabType.values().size

    fun findFragment(position: Int) : BookmarksTabFragment {
        return instantiateItem(viewPager, position) as BookmarksTabFragment
    }

    fun update() {
        for (i in 0 until count) {
            val fragment = findFragment(i)
            fragment.update()
        }
    }

    open fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    }
}
