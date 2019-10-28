package com.suihan74.satena.fragments

import android.os.Bundle
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.adapters.BookmarksAdapter
import com.suihan74.satena.models.BookmarksTabType

class RecentBookmarksTabFragment : BookmarksTabFragment() {
    companion object {
        fun createInstance() = RecentBookmarksTabFragment().apply {
            arguments = Bundle().apply {
                putInt(ARGS_KEY_TAB_TYPE, BookmarksTabType.RECENT.int)
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
