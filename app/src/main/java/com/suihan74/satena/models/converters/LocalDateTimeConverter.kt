package com.suihan74.satena.models.converters

import androidx.room.TypeConverter
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime

class LocalDateTimeConverter {
    private val offset = ZoneOffset.UTC

    @TypeConverter
    fun fromEpochSecond(value: Long?) : LocalDateTime? =
        if (value == null) null else LocalDateTime.ofEpochSecond(value, 0, ZoneOffset.UTC)

    @TypeConverter
    fun toEpochSecond(value: LocalDateTime?) : Long? =
        value?.toEpochSecond(ZonedDateTime.now().offset)
}
