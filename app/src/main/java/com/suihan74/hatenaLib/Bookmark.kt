package com.suihan74.hatenaLib

import com.google.gson.annotations.Expose
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 通常のブクマ情報
 */
data class Bookmark (
    val user : String,
    val comment : String,
    val tags : List<String> = emptyList(),
    val timestamp : LocalDateTime = LocalDateTime.MIN,
    val starCount : List<Star>? = null,
    val private: Boolean = false
) {
    // for Gson
    internal constructor() : this("", "")

    companion object {
        fun create(src: BookmarkWithStarCount) = Bookmark(
            user = src.user,
            comment = src.comment,
            tags = src.tags,
            timestamp = src.timestamp,
            starCount = src.starCount.map { it.toStar() },
            private = src.isPrivate
        )

        fun create(src: BookmarkResult) = Bookmark(
            user = src.user,
            comment = src.comment,
            tags = src.tags,
            timestamp = src.timestamp,
            starCount = src.starsCount,
            private = src.private ?: false
        )
    }

    @delegate:Transient
    val isDummy : Boolean by lazy { timestamp == LocalDateTime.MIN }

    @Expose(serialize = false, deserialize = false)
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

    /**
     * ブックマーク自身を指すURLを取得する
     *
     * スターをつけるときに使用
     */
    fun getBookmarkUrl(entry: Entry) : String {
        val dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd")
        val date = timestamp.format(dateFormat)
        return "${HatenaClient.B_BASE_URL}/$user/$date#bookmark-${entry.id}"
    }

    /**
     * ブコメページのURL
     */
    fun getCommentPageUrl(entry: Entry) : String {
        return "${HatenaClient.B_BASE_URL}/entry/${entry.id}/comment/$user"
    }

    /** タグを含んだコメントを取得する */
    val commentRaw : String get() =
        getTagsText(separator = "") { "[$it]" } + comment

    /** ブコメの中身が更新されていないかを確認する */
    fun same(other: Bookmark?) : Boolean {
        if (other == null) return false
        val starCount = starCount ?: emptyList()
        val otherStarCount = other.starCount ?: emptyList()

        return user == other.user &&
                commentRaw == other.commentRaw &&
                timestamp == other.timestamp &&
                compareStarCount(starCount, otherStarCount) &&
                compareStarCount(otherStarCount, starCount)
    }

    private fun compareStarCount(starCount: List<Star>, other: List<Star>) : Boolean =
        starCount.all { i ->
            other.firstOrNull { o -> o.user == i.user && o.color == i.color }?.count == i.count
        }

    fun toBookmarkWithStarCount(entry: Entry, stars: List<Star>? = null) = BookmarkWithStarCount(
        mUser = BookmarkWithStarCount.User(user, userIconUrl),
        comment = comment,
        isPrivate = private,
        link = getCommentPageUrl(entry),
        tags = tags,
        timestamp = timestamp,
        starCount = (stars ?: starCount)?.groupBy { it.color }
            ?.map { group -> StarCount(group.key, group.value.sumOf { it.count }) }
            .orEmpty()
    )
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
    val success : Boolean? = null,

    @JsonAdapter(BooleanDeserializer::class)
    val private : Boolean? = null,
    val eid : Long? = null,

    val starsCount : List<Star>? = null
) {
    // for Gson
    private constructor() : this("", "", emptyList(), LocalDateTime.MIN, "", "", "")

    fun toBookmarkWithStarCount() = BookmarkWithStarCount(
        mUser = BookmarkWithStarCount.User(user, userIconUrl),
        comment = comment,
        isPrivate = private ?: false,
        link = permalink,
        tags = tags,
        timestamp = timestamp,
        starCount = starsCount.orEmpty().map { StarCount(it.color, it.count) }
    )
}
