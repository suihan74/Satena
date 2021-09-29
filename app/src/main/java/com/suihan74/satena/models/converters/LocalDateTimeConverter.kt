package com.suihan74.satena.models.converters

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class LocalDateTimeConverter {
    private val offset = ZoneOffset.UTC

    @TypeConverter
    fun fromEpochSecond(value: Long?) : LocalDateTime? =
        if (value == null) null else LocalDateTime.ofEpochSecond(value, 0, ZoneOffset.UTC)

    @TypeConverter
    fun toEpochSecond(value: LocalDateTime?) : Long? =
        value?.toEpochSecond(ZonedDateTime.now().offset)
}
