package com.suihan74.hatenaLib

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

data class StarCount (
    @JsonAdapter(StarColorDeserializer::class)
    val color : StarColor,
    val count : Int
) {
    // for Gson
    private constructor() : this(StarColor.Yellow, 0)

    internal fun toStar() = Star(
        user = "",
        quote = "",
        color = color,
        count = count
    )
}

data class BookmarkWithStarCount (
    @SerializedName("user")
    private val mUser : User,
    val comment : String,

    val isPrivate : Boolean,
    val link : String,
    val tags : List<String>,

    @JsonAdapter(TimestampDeserializer::class)
    val timestamp : LocalDateTime,
    val starCount : List<StarCount>
) {

    data class User (
        val name : String,
        val profileImageUrl : String
    ) {
        internal constructor() : this("", "")
    }

    // for Gson
    private constructor() : this(User(), "", false, "", emptyList(), LocalDateTime.MIN, emptyList())

    @delegate:Transient
    val user: String by lazy { mUser.name }


    @delegate:Transient
    val userIconUrl: String by lazy { mUser.profileImageUrl }


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
) {
    // for Gson
    private constructor() : this(null, emptyList(), emptyList())
}
