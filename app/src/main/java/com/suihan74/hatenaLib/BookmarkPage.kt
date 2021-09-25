package com.suihan74.hatenaLib

import android.net.Uri
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class BookmarkPageResponse (
    val bookmark : BookmarkPage
) {
    // for Gson
    private constructor() : this(BookmarkPage())
}

data class FollowingBookmarksResponse (
    val bookmarks : List<FollowingBookmark>
) {
    // for Gson
    private constructor() : this(emptyList())
}

data class BookmarkPage (
    @SerializedName("entry_id")
    val id : Long,
    val entry : Entry,
    @SerializedName("user_name")
    val user : String,
    val comment : BookmarkPageComment,
    val status : String,

    @SerializedName("created_at")
    @JsonAdapter(TimestampDeserializer::class)
    val timestamp : LocalDateTime
) {
    // for Gson
    internal constructor() : this(0, Entry(), "", BookmarkPageComment(), "", LocalDateTime.MIN)

    val permalink : String by lazy {
        val dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd")
        val date = timestamp.format(dateFormat)
        "${HatenaClient.B_BASE_URL}/$user/$date#bookmark-$id"
    }
}

/** お気に入りユーザーのブクマ情報 */
data class FollowingBookmark (
    @SerializedName("entry_id")
    val id : Long,
    val entry : FollowingEntry,
    val userName : String,
    val user : User,
    val comment : BookmarkPageComment,
    val status : String,

    @SerializedName("created_at")
    @JsonAdapter(TimestampDeserializer::class)
    val timestamp : LocalDateTime
) {
    // for Gson
    internal constructor() : this(0, FollowingEntry(), "", User(), BookmarkPageComment(), "", LocalDateTime.MIN)

    val permalink : String by lazy {
        val dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd")
        val date = timestamp.format(dateFormat)
        "${HatenaClient.B_BASE_URL}/$user/$date#bookmark-$id"
    }

    fun toEntry() = Entry(
        id = id,
        title = entry.title,
        description = entry.content,
        count = entry.count,
        url = entry.url,
        rootUrl = Uri.parse(entry.url).authority,
        faviconUrl = entry.faviconUrl,
        _imageUrl = entry.imageUrl,
        ampUrl = entry.ampUrl,
        date = timestamp,
        bookmarkedData = BookmarkResult(
            user = userName,
            comment = comment.body,
            tags = comment.tags,
            timestamp = timestamp,
            userIconUrl = user.profileImageUrl,
            commentRaw = comment.raw,
            permalink = permalink,
            eid = entry.id
        )
    )

    // ------ //

    data class User(
        val name : String,
        val displayName : String,
        val profileImageUrl : String,
        val totalBookmarks : Int,
        val private : Boolean
    ) {
        internal constructor() : this("", "", "", 0, false)
    }
}

data class BookmarkPageComment (
    val body : String,
    val raw : String,
    val tags : List<String>
) {
    // for Gson
    internal constructor() : this("", "", emptyList())
}
