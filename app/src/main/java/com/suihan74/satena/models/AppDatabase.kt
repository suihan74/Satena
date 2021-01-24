package com.suihan74.satena.models

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.suihan74.satena.models.browser.BrowserDao
import com.suihan74.satena.models.browser.HistoryLog
import com.suihan74.satena.models.browser.HistoryPage
import com.suihan74.satena.models.converters.LocalDateTimeConverter
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.TagAndUserRelation
import com.suihan74.satena.models.userTag.User
import com.suihan74.satena.models.userTag.UserTagDao
import org.threeten.bp.OffsetDateTime

/**
 * アプリで使用するDB
 */
@Database(
    entities = [
        User::class,
        Tag::class,
        TagAndUserRelation::class,
        IgnoredEntry::class,
        HistoryPage::class,
        HistoryLog::class
    ],
    version = 6
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userTagDao() : UserTagDao
    abstract fun ignoredEntryDao() : IgnoredEntryDao
    abstract fun browserDao() : BrowserDao
}

// ------ //

/**
 * DBのマイグレーションを設定する
 */
fun RoomDatabase.Builder<AppDatabase>.migrate() : RoomDatabase.Builder<AppDatabase> {
    this.addMigrations(
        // ------ //
        // for development
        Migration1to2(),
        Migration2to3(),
        Migration3to4(),
        Migration4to5(),
        // ------ //
        Migration1to5(),
        Migration5to6()
    )
    .fallbackToDestructiveMigration()

    return this
}

// ------ //

// 2,3,4は開発中に使用していたバージョン

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

/** version 2 to 3 */
class Migration2to3 : Migration(2, 3) {
    private fun createHistoryVisitTimesColumn(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `history` ADD `visitTimes` INTEGER NOT NULL DEFAULT 1")
    }

    override fun migrate(database: SupportSQLiteDatabase) {
        createHistoryVisitTimesColumn(database)
    }
}

/** version 3 to 4 */
class Migration3to4 : Migration(3, 4) {
    private fun createHistoryTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `browser_history_pages` (`url` TEXT NOT NULL, `title` TEXT NOT NULL, `faviconUrl` TEXT NOT NULL, `lastVisited` INTEGER NOT NULL, `visitTimes` INTEGER NOT NULL, `id` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `browser_history_items` (`visitedAt` INTEGER NOT NULL, `pageId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
    }

    private fun dropOldTable(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `history`")
    }

    override fun migrate(database: SupportSQLiteDatabase) {
        createHistoryTables(database)
        dropOldTable(database)
    }
}

/** version 4 to 5 */
class Migration4to5 : Migration(4, 5) {
    private fun createHistoryTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `browser_history_pages` (`url` TEXT NOT NULL, `title` TEXT NOT NULL, `faviconUrl` TEXT NOT NULL, `lastVisited` INTEGER NOT NULL, `visitTimes` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `browser_history_items` (`visitedAt` INTEGER NOT NULL, `pageId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
    }

    private fun dropOldTable(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `browser_history_pages`")
        db.execSQL("DROP TABLE IF EXISTS `browser_history_items`")
    }

    override fun migrate(database: SupportSQLiteDatabase) {
        dropOldTable(database)
        createHistoryTables(database)
    }
}

/** version 1 to 5 */
class Migration1to5 : Migration(1, 5) {
    private fun createHistoryTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `browser_history_pages` (`url` TEXT NOT NULL, `title` TEXT NOT NULL, `faviconUrl` TEXT NOT NULL, `lastVisited` INTEGER NOT NULL, `visitTimes` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `browser_history_items` (`visitedAt` INTEGER NOT NULL, `pageId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
    }

    override fun migrate(database: SupportSQLiteDatabase) {
        createHistoryTables(database)
    }
}

/**
 * v1.5.26: タイムゾーンを考慮に入れる
 *
 * ブラウザ閲覧履歴の時刻(`LocalDateTime`)がすべてUTCに変換しない端末ローカル値で保存されていたので、
 * 端末の現在のタイムゾーンで保存されたと仮定して、その分のオフセットを差し引いたUTC値に修正する
 */
class Migration5to6 : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val offset = OffsetDateTime.now().offset.totalSeconds
        database.execSQL("UPDATE `browser_history_pages` SET `lastVisited` = `lastVisited` - $offset")
        database.execSQL("UPDATE `browser_history_items` SET `visitedAt` = `visitedAt` - $offset")
    }
}
