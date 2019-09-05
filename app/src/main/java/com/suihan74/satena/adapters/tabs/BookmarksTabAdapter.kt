package com.suihan74.satena.adapters.tabs

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.RecyclerView
import com.suihan74.HatenaLib.*
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.satena.fragments.BookmarksTabFragment
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.models.PreferenceKey

open class BookmarksTabAdapter(
        fm : FragmentManager,
        private val context : Context,
        private var allBookmarks : List<Bookmark>,
        private var digest : BookmarksDigest?,
        private var bookmarksEntry : BookmarksEntry,
        private var starsMap : Map<String, StarsEntry>
    ) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        val bookmarks = filterBookmarks(allBookmarks, position)
        return BookmarksTabFragment.createInstance(bookmarks, bookmarksEntry, starsMap, this, BookmarksTabType.fromInt(position))
    }

    override fun getPageTitle(position: Int): CharSequence? = BookmarksTabType.fromInt(position).toString(context)

    override fun getCount() = BookmarksTabType.values().size

    fun filterBookmarks(all : List<Bookmark>, tabPosition : Int) : List<Bookmark> {
        allBookmarks = all

        return when (BookmarksTabType.fromInt(tabPosition)) {
            BookmarksTabType.POPULAR -> {
                digest?.scoredBookmarks?.map { Bookmark.createFrom(it) }
                    ?: allBookmarks.asSequence()
                        .filter { it.comment.isNotEmpty() && !HatenaClient.ignoredUsers.contains(it.user) }
                        .map {
                            val star = starsMap[it.user]
                            Pair(star?.totalStarsCount ?: 0, it)
                        }
                        .sortedByDescending {
                            it.first
                        }
                        .take(10)
                        .map { it.second }.toList()
            }

            BookmarksTabType.RECENT -> {
                allBookmarks.filter { it.comment.isNotEmpty() && !HatenaClient.ignoredUsers.contains(it.user) }
                    .sortedByDescending { it.timestamp }
            }

            else -> {
                val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
                if (prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS)) {
                    allBookmarks
                }
                else {
                    allBookmarks.filter { !HatenaClient.ignoredUsers.contains(it.user) }
                }
            }
        }
    }

    fun findFragment(viewPager: ViewPager, position: Int) : BookmarksTabFragment {
        return instantiateItem(viewPager, position) as BookmarksTabFragment
    }

    open fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    }
}
