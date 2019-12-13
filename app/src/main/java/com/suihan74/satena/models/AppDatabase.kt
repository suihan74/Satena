package com.suihan74.satena.models

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.suihan74.satena.models.converters.LocalDateTimeConverter
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.TagAndUserRelation
import com.suihan74.satena.models.userTag.User
import com.suihan74.satena.models.userTag.UserTagDao

@Database(
    entities = [
        User::class,
        Tag::class,
        TagAndUserRelation::class
    ],
    version = 1
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userTagDao(): UserTagDao
}
