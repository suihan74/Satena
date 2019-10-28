package com.suihan74.satena.scenes.bookmarks

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks.tabs.AllBookmarksTabFragment
import com.suihan74.satena.scenes.bookmarks.tabs.CustomBookmarksTabFragment
import com.suihan74.satena.scenes.bookmarks.tabs.PopularBookmarksTabFragment
import com.suihan74.satena.scenes.bookmarks.tabs.RecentBookmarksTabFragment

open class BookmarksTabAdapter (
    private val bookmarksActivity: BookmarksActivity,
    private val viewPager: ViewPager
) : FragmentPagerAdapter(bookmarksActivity.bookmarksFragment!!.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (BookmarksTabType.fromInt(position)) {
            BookmarksTabType.POPULAR -> PopularBookmarksTabFragment.createInstance()
            BookmarksTabType.RECENT -> RecentBookmarksTabFragment.createInstance()
            BookmarksTabType.CUSTOM -> CustomBookmarksTabFragment.createInstance()
            BookmarksTabType.ALL -> AllBookmarksTabFragment.createInstance()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? = BookmarksTabType.fromInt(position).toString(bookmarksActivity.bookmarksFragment!!.context!!)

    override fun getCount() = BookmarksTabType.values().size

    fun findFragment(position: Int) =
        try {
            instantiateItem(viewPager, position) as? BookmarksTabFragment
        }
        catch (e: Exception) {
            null
        }

    inline fun forEachFragment(action: (BookmarksTabFragment) -> Unit) {
        for (i in 0 until count) {
            val fragment = findFragment(i)
            if (fragment != null) {
                action(fragment)
            }
        }
    }

    fun update() =
        forEachFragment { it.update() }

    fun notifyItemChanged(bookmark: Bookmark) =
        forEachFragment { it.notifyItemChanged(bookmark) }

    fun removeBookmark(bookmark: Bookmark) =
        forEachFragment { it.removeBookmark(bookmark) }

    open fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    }
}
