package com.suihan74.satena.scenes.entries

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.HatenaLib.BookmarkResult
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.Star
import com.suihan74.HatenaLib.StarsEntry
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

abstract class StarsFragmentBase : MultipurposeSingleTabEntriesFragment() {
    data class BookmarkCommentUrl (
        val url : String,
        val user : String,
        val timestamp : String,
        val eid : Long,
        val starsCount: List<Star>
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!
        refreshEntries()
        return root
    }

    fun refreshEntries(fetchingTask: Deferred<List<StarsEntry>>) =
        super.refreshEntries("スターの取得失敗") {
            async(Dispatchers.IO) {
                val urlRegex =
                    Regex("""https?://b\.hatena\.ne\.jp/(.+)/(\d+)#bookmark\-(\d+)""")
                val entries = fetchingTask.await()
                val data =
                    entries.mapNotNull {
                        val match = urlRegex.matchEntire(it.url) ?: return@mapNotNull null
                        val user = match.groups[1]?.value ?: return@mapNotNull null
                        val timestamp = match.groups[2]?.value ?: return@mapNotNull null
                        val eid = match.groups[3]?.value ?: return@mapNotNull null
                        val starsCount = it.allStars.groupBy { s -> s.color }.map { s -> Star("", "", s.key, s.value.size) }
                        BookmarkCommentUrl(
                            it.url,
                            user,
                            timestamp,
                            eid.toLong(),
                            starsCount
                        )
                    }
                    .distinctBy { it.eid }

                val tasks = data.map { HatenaClient.getBookmarkPageAsync(it.eid, it.user) }
                tasks.awaitAll()

                return@async tasks.mapIndexedNotNull { index, deferred ->
                    val eid = data[index].eid
                    try {
                        val bookmark = deferred.await()
                        val bookmarkedData = BookmarkResult(
                            user = bookmark.user,
                            comment = bookmark.comment.body,
                            tags = bookmark.comment.tags,
                            timestamp = bookmark.timestamp,
                            userIconUrl = HatenaClient.getUserIconUrl(bookmark.user),
                            commentRaw = bookmark.comment.raw,
                            permalink = data[index].url,
                            success = true,
                            private = false,
                            eid = eid,
                            starsCount = data[index].starsCount
                        )
                        bookmark.entry.copy(
                            rootUrl = Uri.parse(bookmark.entry.url)?.encodedPath ?: bookmark.entry.url,
                            bookmarkedData = bookmarkedData
                        )
                    }
                    catch (e: Exception) {
                        null
                    }
                }
            }
        }
}
