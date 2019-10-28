package com.suihan74.satena.fragments

import android.os.Bundle
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.adapters.BookmarksAdapter
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

class AllBookmarksTabFragment : BookmarksTabFragment() {
    private var mIsIgnoredUsersShownInAll : Boolean = false

    companion object {
        fun createInstance() = AllBookmarksTabFragment().apply {
            arguments = Bundle().apply {
                putInt(ARGS_KEY_TAB_TYPE, BookmarksTabType.ALL.int)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        mIsIgnoredUsersShownInAll = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS)
    }

    override fun getBookmarks(fragment: BookmarksFragment) =
        fragment.recentBookmarks.let { bookmarks ->
            if (mIsIgnoredUsersShownInAll) {
                bookmarks
            }
            else {
                bookmarks.filterNot {
                    fragment.ignoredUsers.contains(it.user)
                }
            }.filterNot { isBookmarkIgnored(it) }
        }

    override fun isBookmarkShown(bookmark: Bookmark, fragment: BookmarksFragment) : Boolean {
//        val contains = fragment.bookmarksEntry?.bookmarks?.any { it.user == bookmark.user } == true
        return (!fragment.ignoredUsers.contains(bookmark.user) || mIsIgnoredUsersShownInAll) && !isBookmarkIgnored(bookmark)
    }

    override fun hideIgnoredBookmark(adapter: BookmarksAdapter, bookmark: Bookmark) {
        if (!mIsIgnoredUsersShownInAll) {
            adapter.removeItem(bookmark)
        }
    }
}
