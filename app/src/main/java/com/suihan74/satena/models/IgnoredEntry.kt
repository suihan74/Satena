package com.suihan74.satena.models

import com.suihan74.hatenaLib.Entry
import org.threeten.bp.LocalDateTime
import java.io.Serializable

@Deprecated("DBに移行")
enum class IgnoredEntryType {
    TEXT,
    URL
}

@Deprecated("DBに移行")
@Suppress("deprecation")
enum class IgnoreTarget(val int: Int) {
    NONE(0),
    ENTRY(1),
    BOOKMARK(2),
    ALL(3);

    companion object {
        fun fromInt(i: Int) = values().first { it.int == i }
    }

    infix fun or(other: IgnoreTarget) = fromInt(int or other.int)
    infix fun contains(other: IgnoreTarget) : Boolean = 0 != (int and other.int)
}

@Deprecated("DBに移行")
@Suppress("deprecation")
data class IgnoredEntry (
    val type: IgnoredEntryType,
    val query: String,
    val target: IgnoreTarget = IgnoreTarget.ALL,
    val createdAt: LocalDateTime = LocalDateTime.now()
) : Serializable {

    fun isMatched(entry: Entry) = when (type) {
        IgnoredEntryType.URL -> entry.url.startsWith("https://$query") || entry.url.startsWith("http://$query")
        IgnoredEntryType.TEXT -> target.contains(IgnoreTarget.ENTRY) && entry.title.contains(query)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is IgnoredEntry) return false
        return type == other.type && query == other.query
    }

    override fun hashCode(): Int {
        return (type.hashCode() + query.hashCode()) * 31
    }
}