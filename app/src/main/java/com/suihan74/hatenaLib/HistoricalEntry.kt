package com.suihan74.hatenaLib

import com.google.gson.annotations.JsonAdapter
import org.threeten.bp.LocalDateTime

/** ユーザーのブクマの場合とはてな全体とで微妙に仕様が違うので注意 */
internal data class HistoricalEntry (
    /** ユーザーではnull */
    val id: Long?,

    val title: String,

    /** はてな全体ではnull */
    val totalBookmarks: Int?,

    val rootUrl: String,

    val canonicalUrl: String,

    val faviconUrl: String,

    val categoryCssClassName: String,

    /** ユーザーではnull */
    @JsonAdapter(TimestampDeserializer::class)
    val createdAt: LocalDateTime?
) {
    internal constructor() : this(null, "", null, "", "", "", "", null)

    fun toEntry(bookmarkedData: BookmarkResult? = null, count: Int? = null) = Entry(
        id = id ?: 0L,
        title = title,
        description = "",
        count = totalBookmarks ?: count ?: 0,
        url = canonicalUrl,
        rootUrl = rootUrl,
        faviconUrl = faviconUrl,
        bookmarkedData = bookmarkedData
    )
}

internal data class HatenaHistoricalEntry (
    val entries: List<HistoricalEntry>
) {
    private constructor() : this(emptyList())
}

internal data class UserHistoricalEntry (
    val entry: HistoricalEntry,

    @JsonAdapter(TimestampDeserializer::class)
    val createdAt: LocalDateTime,

    val commentExpanded: String,

    val userIconUrl: String
) {
    private constructor() : this(HistoricalEntry(), LocalDateTime.MIN, "", "")

    fun toEntry(user: String) = entry.toEntry(
        bookmarkedData = BookmarkResult(
            user = user,
            comment = commentExpanded,
            tags = emptyList(),
            timestamp = createdAt,
            userIconUrl = userIconUrl,
            commentRaw = commentExpanded,
            permalink = "",
        )
    )
}
