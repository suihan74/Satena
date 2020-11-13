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

/** ipad.entry_bookmarks.jsonのレスポンス */
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

    /** 内容を文字列化する */
    @delegate:Transient
    val string: String by lazy {
        // 改行は使用されていないはずなので、区切り文字として使う
        val dateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm:ss")
        user + "\n" + comment + "\n" + tags.joinToString("\n", postfix = "\n") + timestamp.format(dateFormat)
    }

    // ------ //

    fun getBookmarkUrl(entry: Entry) : String {
        val dateFormat = DateTimeFormatter.ofPattern("yyyMMdd")
        val date = timestamp.format(dateFormat)
        return "${HatenaClient.B_BASE_URL}/$user/$date#bookmark-${entry.id}"
    }
}

/** 追加ロードのためのカーソルを含んだブコメリスト取得用のレスポンス */
data class BookmarksWithCursor (
    val cursor: String?,
    val bookmarks: List<BookmarkWithStarCount>
)

/** 人気コメントを取得するためのレスポンス */
data class BookmarksDigest (
    val referedBlogEntries : List<Entry>,
    val scoredBookmarks : List<BookmarkWithStarCount>,
    val favoriteBookmarks : List<BookmarkWithStarCount>
) {
    // for Gson
    private constructor() : this(emptyList(), emptyList(), emptyList())
}
