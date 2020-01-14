package com.suihan74.satena.models.converters

import androidx.room.TypeConverter
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset

class LocalDateTimeConverter {
    private val offset = ZoneOffset.UTC

    @TypeConverter
    fun fromTimestamp(value: Long?) : LocalDateTime? =
        if (value == null) null else LocalDateTime.ofEpochSecond(value, 0, offset)

    @TypeConverter
    fun toTimestamp(value: LocalDateTime?) : Long? =
        value?.toEpochSecond(offset)
}
