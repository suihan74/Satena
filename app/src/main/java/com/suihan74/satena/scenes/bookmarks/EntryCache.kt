package com.suihan74.satena.scenes.bookmarks

import com.suihan74.hatenaLib.BookmarkWithStarCount
import com.suihan74.hatenaLib.BookmarksDigest
import com.suihan74.hatenaLib.BookmarksEntry
import com.suihan74.hatenaLib.Entry

data class EntryCache(
    val entry : Entry,
    val bookmarksEntry : BookmarksEntry,
    val bookmarksDigest : BookmarksDigest?,
    val bookmarksRecentCache : List<BookmarkWithStarCount>,
    val recentCursor : String?,
)
