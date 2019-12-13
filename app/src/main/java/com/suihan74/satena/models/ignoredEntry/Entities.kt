package com.suihan74.satena.models.ignoredEntry

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.suihan74.HatenaLib.Entry
import org.threeten.bp.LocalDateTime
import java.io.Serializable

@Entity(tableName = "ignored_entry")
data class IgnoredEntry (
    @TypeConverters(IgnoredEntryTypeConverter::class)
    val type: IgnoredEntryType,

    val query: String,

    @TypeConverters(IgnoreTargetConverter::class)
    val target: IgnoreTarget = IgnoreTarget.ALL,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    @PrimaryKey
    val id: Int = 0
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

// ================================================= //

enum class IgnoredEntryType {
    TEXT,
    URL
}

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

class IgnoredEntryTypeConverter {
    @TypeConverter
    fun fromInt(value: Int?) =
        if (value == null) null else IgnoredEntryType.values()[value]

    @TypeConverter
    fun toInt(value: IgnoredEntryType?) = value?.ordinal
}

class IgnoreTargetConverter {
    @TypeConverter
    fun fromInt(value: Int?) =
        if (value == null) null else IgnoreTarget.fromInt(value)

    @TypeConverter
    fun toInt(value: IgnoreTarget?) = value?.int
}
