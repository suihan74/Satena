package com.suihan74.satena.scenes.bookmarks.tabs

import android.os.Bundle
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks.BookmarksAdapter
import com.suihan74.satena.scenes.bookmarks.BookmarksFragment
import com.suihan74.satena.scenes.bookmarks.BookmarksTabFragment

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
