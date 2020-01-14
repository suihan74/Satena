package com.suihan74.satena.models

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.invokeSuspend
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.Continuation

typealias DBIgnoredEntryType = com.suihan74.satena.models.ignoredEntry.IgnoredEntryType
typealias DBIgnoredTarget = com.suihan74.satena.models.ignoredEntry.IgnoreTarget

@Suppress("NonAsciiCharacters", "SpellCheckingInspection", "DEPRECATION")
class IgnoredEntriesKeyMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    private val legacyEntries = listOf(
        IgnoredEntry(IgnoredEntryType.URL, "google.com"),
        IgnoredEntry(IgnoredEntryType.URL, "twitter.com"),
        IgnoredEntry(IgnoredEntryType.TEXT, "hoge"),
        IgnoredEntry(IgnoredEntryType.TEXT, "FUGA"),
        IgnoredEntry(IgnoredEntryType.TEXT, "all", IgnoreTarget.ALL),
        IgnoredEntry(IgnoredEntryType.TEXT, "entry", IgnoreTarget.ENTRY),
        IgnoredEntry(IgnoredEntryType.TEXT, "bookmark", IgnoreTarget.BOOKMARK)
    )

    private var previousVersion: Int = 0

    /** テストデータの作成 */
    @Before
    fun initialize() {
        val prefs = SafeSharedPreferences.create<IgnoredEntriesKeyVersion1>(context)
        prefs.edit {
            put(IgnoredEntriesKeyVersion1.IGNORED_ENTRIES, legacyEntries)
        }
        previousVersion = prefs.version

        val dao = db.ignoredEntryDao()
        dao.clearAllEntries()
    }

    @Test
    fun sharedPreferencesからRoomDBへの移行() {
        val prefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context)
        val dao = db.ignoredEntryDao()

        // テスト対象メソッドを取得
        val migrationMethod = IgnoredEntriesKeyMigration::class.java.getDeclaredMethod(
            "migrateFromVersion1",
            SafeSharedPreferences::class.java,
            IgnoredEntryDao::class.java,
            Continuation::class.java
        ).apply {
            isAccessible = true
        }

        runBlocking {
            migrationMethod.invokeSuspend(IgnoredEntriesKeyMigration, prefs, dao)
        }

        // prefsのバージョンを上げて次回以降の移行処理を実行しないようにされているか
        assertTrue(prefs.version > previousVersion)

        // prefsに存在したデータが全てdbに存在しているか
        assertEquals(legacyEntries.size, dao.getAllEntries().count())
        legacyEntries.forEach {
            assertNotNull(
                dao.find(DBIgnoredEntryType.valueOf(it.type.name), it.query)
            )
        }

        val targetAll = dao.find(DBIgnoredEntryType.TEXT, "all")!!
        assertEquals(DBIgnoredTarget.ALL, targetAll.target)

        val targetEntry = dao.find(DBIgnoredEntryType.TEXT, "entry")!!
        assertEquals(DBIgnoredTarget.ENTRY, targetEntry.target)

        val targetBookmark = dao.find(DBIgnoredEntryType.TEXT, "bookmark")!!
        assertEquals(DBIgnoredTarget.BOOKMARK, targetBookmark.target)
    }
}
