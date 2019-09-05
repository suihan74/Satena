package com.suihan74.HatenaLib

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.Serializable

/**
 * 通常のブクマ情報
 */
data class Bookmark (
    val user : String,
    val comment : String,
    val tags : List<String>,
    val timestamp : LocalDateTime,
    val starCount : List<Star>?
) : Serializable {
    companion object {
        fun createFrom(src: BookmarkWithStarCount) = Bookmark(
            user = src.user.name,
            comment = src.comment,
            tags = src.tags,
            timestamp = src.timestamp,
            starCount = src.starCount)
    }

    var mUserIconUrl: String? = null
    val userIconUrl : String
        get() {
            if (mUserIconUrl == null) {
                mUserIconUrl = HatenaClient.getUserIconUrl(user)
            }
            return mUserIconUrl!!
        }

    fun getTagsText(
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        postfix: CharSequence = "",
        limit: Int = -1,
        truncated: CharSequence = "...",
        transform: ((String)->CharSequence)? = null
    ) = tags.joinToString(separator, prefix, postfix, limit, truncated, transform)

    // ブックマーク自身を指すURLを取得する
    // ブコメについたスターを取得する際に使用する
    fun getBookmarkUrl(entry: BookmarksEntry) : String {
        val dateFormat = DateTimeFormatter.ofPattern("yyyMMdd")
        val date = timestamp.format(dateFormat)
        return "${HatenaClient.B_BASE_URL}/$user/$date#bookmark-${entry.id}"
//    return "http://b.hatena.ne.jp/entry/${entry.id}/comment/$user"
    }

    fun getBookmarkUrl(entry: Entry) : String {
        val dateFormat = DateTimeFormatter.ofPattern("yyyMMdd")
        val date = timestamp.format(dateFormat)
        return "${HatenaClient.B_BASE_URL}/$user/$date#bookmark-${entry.id}"
    }
}

/**
 * ブクマした際に返ってくる結果 + マイホットエントリコメント
 */
data class BookmarkResult (
    val user : String,
    val comment : String,
    val tags : List<String>,

    @SerializedName("epoch")
    @JsonAdapter(EpochTimeDeserializer::class)
    val timestamp : LocalDateTime,

    val userIconUrl : String,
    val commentRaw : String,  // タグ文字列( [tag] )を含むコメント
    val permalink : String,

    // 以下、ブクマリザルトとして扱われる場合のみ含まれる

    @JsonAdapter(BooleanDeserializer::class)
    val success : Boolean?,

    @JsonAdapter(BooleanDeserializer::class)
    val private : Boolean?,
    val eid : Long?
) : Serializable



fun BookmarkResult.getBookmarkUrl() : String {
    val dateFormat = DateTimeFormatter.ofPattern("yyyMMdd")
    val date = timestamp.format(dateFormat)
    return "${HatenaClient.B_BASE_URL}/$user/$date#bookmark-$eid"
}
