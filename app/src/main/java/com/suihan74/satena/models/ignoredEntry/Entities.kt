package com.suihan74.satena.models.ignoredEntry

import androidx.room.*
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.BookmarkWithStarCount
import com.suihan74.hatenaLib.Entry

@Entity(
    tableName = "ignored_entry",
    indices = [Index(value = ["type", "query"], name = "ignoredEntry_type_query", unique = true)]
)
@TypeConverters(
    IgnoredEntryTypeConverter::class,
    IgnoreTargetConverter::class
)
data class IgnoredEntry (
    val type: IgnoredEntryType = IgnoredEntryType.URL,

    val query: String = "",

    val target: IgnoreTarget = IgnoreTarget.ALL,

    val asRegex: Boolean = false,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
) {
    companion object {
        /** 登録前のダミーデータを作成する */
        fun createDummy(
            type: IgnoredEntryType = IgnoredEntryType.URL,
            query: String = "",
            target: IgnoreTarget = IgnoreTarget.ALL,
            isRegex: Boolean = false
        ) : IgnoredEntry =
            IgnoredEntry(
                type = type,
                query = query,
                target = target,
                asRegex = isRegex,
                id = -1
            )
    }

    @delegate:Transient
    @delegate:Ignore
    private val regex by lazy { Regex(query) }

    // ------ //

    override fun equals(other: Any?): Boolean {
        if (other !is IgnoredEntry) return false
        return type == other.type && query == other.query && asRegex == other.asRegex
    }

    override fun hashCode(): Int {
        return (type.hashCode() + query.hashCode() + asRegex.hashCode()) * 31
    }

    // ------ //

    fun match(entry: Entry) : Boolean =
        if (asRegex) matchWithRegex(entry)
        else matchWithPlain(entry)

    private fun matchWithRegex(entry: Entry) : Boolean = when (type) {
        IgnoredEntryType.URL -> {
            entry.adUrl?.let { adUrl ->
                regex.containsMatchIn(entry.url) || regex.containsMatchIn(adUrl)
            } ?: regex.containsMatchIn(entry.url)
        }

        IgnoredEntryType.TEXT -> {
            target.contains(IgnoreTarget.ENTRY) && regex.containsMatchIn(entry.title)
        }
    }

    private fun matchWithPlain(entry: Entry) : Boolean = when (type) {
        IgnoredEntryType.URL -> {
            entry.adUrl?.let { adUrl ->
                matchPlainUrl(entry.url) || matchPlainUrl(adUrl)
            } ?: matchPlainUrl(entry.url)
        }

        IgnoredEntryType.TEXT -> {
            target.contains(IgnoreTarget.ENTRY) && entry.title.contains(query)
        }
    }

    private fun matchPlainUrl(url: String) : Boolean {
        val targetUrl = when {
            url.startsWith("https://") -> "https://$query"
            else -> "http://$query"
        }
        return url.startsWith(targetUrl)
    }

    // ------ //

    fun match(bookmark: BookmarkWithStarCount) : Boolean =
        if (asRegex) matchWithRegex(user = bookmark.user, comment = bookmark.comment, tags = bookmark.tags)
        else matchWithPlain(user = bookmark.user, comment = bookmark.comment, tags = bookmark.tags)

    fun match(bookmark: Bookmark) : Boolean =
        if (asRegex) matchWithRegex(user = bookmark.user, comment = bookmark.comment, tags = bookmark.tags)
        else matchWithPlain(user = bookmark.user, comment = bookmark.comment, tags = bookmark.tags)

    private fun matchWithRegex(user: String, comment: String, tags: List<String>) : Boolean = when(type) {
        IgnoredEntryType.URL -> {
            target.contains(IgnoreTarget.BOOKMARK)
                    && (comment.contains("https://$query") || comment.contains("http://$query"))
        }

        IgnoredEntryType.TEXT -> {
            target.contains(IgnoreTarget.BOOKMARK)
                    && (
                    regex.containsMatchIn(comment)
                            || regex.containsMatchIn(user)
                            || tags.any { regex.containsMatchIn(it) }
                    )
        }
    }

    private fun matchWithPlain(user: String, comment: String, tags: List<String>) : Boolean = when(type) {
        IgnoredEntryType.URL -> {
            target.contains(IgnoreTarget.BOOKMARK)
                    && (comment.contains("https://$query") || comment.contains("http://$query"))
        }

        IgnoredEntryType.TEXT -> {
            target.contains(IgnoreTarget.BOOKMARK)
                    && (
                    comment.contains(query)
                            || user.contains(query)
                            || tags.any { it.contains(query) }
                    )
        }
    }
}

// ================================================= //

enum class IgnoredEntryType {
    TEXT,
    URL;

    companion object {
        fun fromOrdinal(idx: Int) = values().getOrElse(idx) { TEXT }
    }
}

enum class IgnoreTarget(val id: Int) {
    NONE(0),
    ENTRY(1),
    BOOKMARK(2),
    ALL(3);

    companion object {
        fun fromId(i: Int) = values().first { it.id == i }
    }

    infix fun or(other: IgnoreTarget) = fromId(id or other.id)
    infix fun contains(other: IgnoreTarget) : Boolean = 0 != (id and other.id)
}

class IgnoredEntryTypeConverter {
    @TypeConverter
    fun fromInt(value: Int?) = value?.let { IgnoredEntryType.fromOrdinal(it) }

    @TypeConverter
    fun toInt(value: IgnoredEntryType?) = value?.ordinal
}

class IgnoreTargetConverter {
    @TypeConverter
    fun fromInt(value: Int?) = value?.let { IgnoreTarget.fromId(it) }

    @TypeConverter
    fun toInt(value: IgnoreTarget?) = value?.id
}
