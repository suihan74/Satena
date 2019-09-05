package com.suihan74.HatenaLib

import com.google.gson.annotations.JsonAdapter
import org.threeten.bp.LocalDateTime
import java.io.Serializable

data class User (
    val name : String,
    val profileImageUrl : String
) : Serializable

data class StarCount (
    @JsonAdapter(StarColorDeserializer::class)
    val color : StarColor,
    val count : Int
) : Serializable {

    internal fun toStar() = Star(
        user = "",
        quote = "",
        color = color,
        count = count
    )

}

data class BookmarkWithStarCount (
    private val mUser : User,
    val comment : String,

    val isPrivate : Boolean,
    val link : String,
    val tags : List<String>,

    @JsonAdapter(TimestampDeserializer::class)
    val timestamp : LocalDateTime,
    val starCount : List<StarCount>
) : Serializable {

    val user: String
        get() = mUser.name

    val userIconUrl: String
        get() = mUser.profileImageUrl

}

data class BookmarksDigest (
    val referredBlogEntries : List<Entry>,
    val scoredBookmarks : List<BookmarkWithStarCount>,
    val favoriteBookmarks : List<BookmarkWithStarCount>
) : Serializable

