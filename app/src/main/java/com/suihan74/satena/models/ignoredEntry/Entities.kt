package com.suihan74.satena.models.ignoredEntry

import androidx.room.*
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
    var type: IgnoredEntryType = IgnoredEntryType.URL,

    var query: String = "",

    var target: IgnoreTarget = IgnoreTarget.ALL,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
) {
    companion object {
        /** 登録前のダミーデータを作成する */
        fun createDummy(
            type: IgnoredEntryType = IgnoredEntryType.URL,
            query: String = "",
            target: IgnoreTarget = IgnoreTarget.ALL
        ) : IgnoredEntry =
            IgnoredEntry(type, query, target, id = -1)
    }

    // ------ //

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
