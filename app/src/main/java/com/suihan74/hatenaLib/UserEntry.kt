package com.suihan74.hatenaLib

import android.net.Uri
import org.threeten.bp.LocalDateTime
import java.io.Serializable

data class UserEntryComment (
    val raw : String,
    val tags : List<String>,
    val body : String
) : Serializable

data class UserEntryBody (
    val totalBookmarks : Int,
    val entryId : Long,
    val createdAt : LocalDateTime,
    val imageUrl : String?,
    val faviconUrl : String?,
    val content : String,
    val title : String,
    val url : String
) : Serializable

data class UserEntry (
    val comment : UserEntryComment,
    val entry : UserEntryBody,
    val entryId : Long,
    val createdAt: LocalDateTime,
    val userName : String,
    val status : String // public/private
) : Serializable {

    fun toEntry() : Entry = Entry(
        id = entryId,
        title = entry.title,
        description = entry.content,
        count = entry.totalBookmarks,
        url = entry.url,
        faviconUrl = entry.faviconUrl ?: "",
        imageUrl = entry.imageUrl ?: "",
        rootUrl = Uri.parse(entry.url).let { it.scheme!! + "://" + it.host!! },
        bookmarkedData = BookmarkResult(
            user = userName,
            comment = comment.body,
            tags = comment.tags,
            timestamp = createdAt,
            userIconUrl = HatenaClient.getUserIconUrl(userName),
            commentRaw = comment.raw,
            permalink = "https://b.hatena.ne.jp/%s/%04d%02d%02d#bookmark-%d".format(userName, createdAt.year, createdAt.monthValue, createdAt.dayOfMonth, entryId),
            success = true,
            private = status == "private",
            eid = entryId
        )
    )

}

internal data class UserEntryResponse (
    val bookmarks : List<UserEntry>
) : Serializable
