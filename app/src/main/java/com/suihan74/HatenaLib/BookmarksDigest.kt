package com.suihan74.HatenaLib

import com.google.gson.annotations.JsonAdapter
import org.threeten.bp.LocalDateTime
import java.io.Serializable

data class User (
    val name : String,
    val profileImageUrl : String
) : Serializable

data class BookmarkWithStarCount (
    val user : User,
    val comment : String,

    val isPrivate : Boolean,
    val link : String,
    val tags : List<String>,

    @JsonAdapter(TimestampDeserializer::class)
    val timestamp : LocalDateTime,
    val starCount : List<Star>
) : Serializable

data class BookmarksDigest (
    val referredBlogEntries : List<Entry>,
    val scoredBookmarks : List<BookmarkWithStarCount>,
    val favoriteBookmarks : List<BookmarkWithStarCount>
) : Serializable

