package com.suihan74.hatenaLib

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.Serializable

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

class BookmarkWithStarCount (
    user : User,
    val comment : String,

    val isPrivate : Boolean,
    val link : String,
    val tags : List<String>,

    @JsonAdapter(TimestampDeserializer::class)
    val timestamp : LocalDateTime,
    val starCount : List<StarCount>
) : Serializable {

    data class User (
        val name : String,
        val profileImageUrl : String
    ) : Serializable

    @SerializedName("user")
    private val mUser : User = user

    val user: String
        get() = mUser.name

    val userIconUrl: String
        get() = mUser.profileImageUrl


    fun getBookmarkUrl(entry: Entry) : String {
        val dateFormat = DateTimeFormatter.ofPattern("yyyMMdd")
        val date = timestamp.format(dateFormat)
        return "${HatenaClient.B_BASE_URL}/$user/$date#bookmark-${entry.id}"
    }
}

data class BookmarksDigest (
    val referredBlogEntries : List<Entry>?,
    val scoredBookmarks : List<BookmarkWithStarCount>,
    val favoriteBookmarks : List<BookmarkWithStarCount>
) : Serializable
