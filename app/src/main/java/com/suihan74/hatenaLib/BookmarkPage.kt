package com.suihan74.hatenaLib

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime

data class BookmarkPageResponse (
    val bookmark : BookmarkPage
)

data class FollowingBookmarksResponse (
    val bookmarks : List<BookmarkPage>
)

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
)

data class BookmarkPageComment (
    val body : String,
    val raw : String,
    val tags : List<String>
)
