package com.suihan74.satena.models.converters

import androidx.room.TypeConverter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ZonedDateTimeConverter {
    @TypeConverter
    fun fromEpochSecond(value: Long?) : ZonedDateTime? =
        value?.let {
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        }

    @TypeConverter
    fun toEpochSecond(value: ZonedDateTime?) : Long? =
        value?.toInstant()?.toEpochMilli()
}
