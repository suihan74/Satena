package com.suihan74.satena.scenes.bookmarks.tabs

import android.os.Bundle
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks.BookmarksAdapter
import com.suihan74.satena.scenes.bookmarks.BookmarksFragment
import com.suihan74.satena.scenes.bookmarks.BookmarksTabFragment

class RecentBookmarksTabFragment : BookmarksTabFragment() {
    companion object {
        fun createInstance() = RecentBookmarksTabFragment().apply {
            arguments = Bundle().apply {
                putInt(ARGS_KEY_TAB_TYPE, BookmarksTabType.RECENT.ordinal)
            }
        }
    }

    override fun getBookmarks(fragment: BookmarksFragment) =
        fragment.recentBookmarks.filter { isBookmarkShown(it, fragment) }

    override fun isBookmarkShown(bookmark: Bookmark, fragment: BookmarksFragment) =
        !bookmark.comment.isBlank() && !isBookmarkIgnored(bookmark) && !fragment.ignoredUsers.contains(bookmark.user)

    override fun hideIgnoredBookmark(adapter: BookmarksAdapter, bookmark: Bookmark) =
        adapter.removeItem(bookmark)
}
