package com.suihan74.hatenaLib

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

data class BookmarkPageResponse (
    val bookmark : BookmarkPage
) {
    // for Gson
    private constructor() : this(BookmarkPage())
}

data class FollowingBookmarksResponse (
    val bookmarks : List<BookmarkPage>
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

data class BookmarkPageComment (
    val body : String,
    val raw : String,
    val tags : List<String>
) {
    // for Gson
    internal constructor() : this("", "", emptyList())
}
