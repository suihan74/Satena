package com.suihan74.satena.models

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.suihan74.satena.models.browser.BrowserDao
import com.suihan74.satena.models.browser.History
import com.suihan74.satena.models.converters.LocalDateTimeConverter
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.TagAndUserRelation
import com.suihan74.satena.models.userTag.User
import com.suihan74.satena.models.userTag.UserTagDao

@Database(
    entities = [
        User::class,
        Tag::class,
        TagAndUserRelation::class,
        IgnoredEntry::class,
        History::class
    ],
    version = 2
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userTagDao() : UserTagDao
    abstract fun ignoredEntryDao() : IgnoredEntryDao
    abstract fun browserDao() : BrowserDao
}

/** version 1 to 2 */
class Migration1to2 : Migration(1, 2) {
    private fun createHistoryTable(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `history` (`url` TEXT NOT NULL, `title` TEXT NOT NULL, `faviconUrl` TEXT NOT NULL, `lastVisited` INTEGER NOT NULL, PRIMARY KEY(`url`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `url` ON `history` (`url`)")
    }

    override fun migrate(database: SupportSQLiteDatabase) {
        createHistoryTable(database)
    }
}
