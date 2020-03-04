package com.suihan74.satena.scenes.entries

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Star
import com.suihan74.hatenaLib.StarsEntry
import com.suihan74.satena.R
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

abstract class StarsFragmentBase : SingleTabEntriesFragmentBase() {
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
        super.refreshEntries(getString(R.string.msg_get_stars_report_failed)) {
            async(Dispatchers.IO) {
                val urlRegex =
                    Regex("""https?://b\.hatena\.ne\.jp/(.+)/(\d+)#bookmark-(\d+)""")
                val entries = fetchingTask.await()
                val data = entries
                    .mapNotNull {
                        val match = urlRegex.matchEntire(it.url) ?: return@mapNotNull null
                        val user = match.groups[1]?.value ?: return@mapNotNull null
                        val timestamp = match.groups[2]?.value ?: return@mapNotNull null
                        val eid = match.groups[3]?.value ?: return@mapNotNull null
                        val starsCount = it.allStars
                        BookmarkCommentUrl(
                            it.url,
                            user,
                            timestamp,
                            eid.toLong(),
                            starsCount
                        )
                    }
                    .groupBy { it.eid.toString() + "_" + it.user }
                    .map {
                        val starsCount = it.value
                            .flatMap { e -> e.starsCount }
                            .groupBy { s -> s.color }
                            .map { s -> Star("", "", s.key, s.value.size) }
                        it.value.first().copy(
                            starsCount = starsCount
                        )
                    }

                val tasks = data.map { HatenaClient.getBookmarkPageAsync(it.eid, it.user) }
                try {
                    tasks.awaitAll()
                }
                catch (e: Throwable) {
                    Log.d("MyStarsFragment", e.message)
                    e.printStackTrace()
                }

                return@async tasks.mapIndexedNotNull { index, deferred ->
                    // TODO: 現状だとブクマ消されたらエントリも表示されなくなる
                    if (deferred.isCancelled) return@mapIndexedNotNull null

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
                }.groupBy { it.id }
                 .map {
                     it.value.first().copy(
                         bookmarkedData = null,
                         myhotentryComments = it.value.mapNotNull { e -> e.bookmarkedData }
                     )
                 }
            }
        }
}
