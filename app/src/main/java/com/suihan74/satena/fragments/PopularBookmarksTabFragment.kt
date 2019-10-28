package com.suihan74.satena.fragments

import android.os.Bundle
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.adapters.BookmarksAdapter
import com.suihan74.satena.models.BookmarksTabType

class PopularBookmarksTabFragment : BookmarksTabFragment() {
    companion object {
        fun createInstance() = PopularBookmarksTabFragment().apply {
            arguments = Bundle().apply {
                putInt(ARGS_KEY_TAB_TYPE, BookmarksTabType.POPULAR.int)
            }
        }
    }

    override val isScrollingUpdaterEnabled = false

    override fun getBookmarks(fragment: BookmarksFragment) =
        fragment.popularBookmarks.filter {
            !isBookmarkIgnored(it) && !fragment.ignoredUsers.contains(it.user)
        }

    override fun isBookmarkShown(bookmark: Bookmark, fragment: BookmarksFragment) =
        fragment.popularBookmarks.any { it.user == bookmark.user } && !isBookmarkIgnored(bookmark) && !fragment.ignoredUsers.contains(bookmark.user)

    override fun hideIgnoredBookmark(adapter: BookmarksAdapter, bookmark: Bookmark) =
        adapter.removeItem(bookmark)
}
