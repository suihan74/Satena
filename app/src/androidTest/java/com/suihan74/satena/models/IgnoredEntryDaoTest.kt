package com.suihan74.satena.models

import android.util.Log
import androidx.room.Room
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.suihan74.satena.models.ignoredEntry.*
import com.suihan74.satena.models.ignoredEntry.IgnoreTarget
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@Suppress("NonAsciiCharacters")
@RunWith(AndroidJUnit4ClassRunner::class)
class IgnoredEntryDaoTest {
    // Context of the app under test.
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val db =
        Room.inMemoryDatabaseBuilder(
            appContext, AppDatabase::class.java).build()
    private val dao = db.ignoredEntryDao()

    @Before
    fun データ初期化() {
        dao.run {
            clearAllEntries()
            insert(IgnoredEntry(IgnoredEntryType.URL, "www.google.co.jp"))
            insert(IgnoredEntry(IgnoredEntryType.URL, "twitter.com"))
            insert(IgnoredEntry(IgnoredEntryType.TEXT, "HOGE"))
            insert(IgnoredEntry(IgnoredEntryType.TEXT, "ハゲ", IgnoreTarget.ENTRY))
        }
        Log.i("initialized", "データ初期化")
    }

    @Test
    fun find動作確認() {
        val google = dao.find(IgnoredEntryType.URL, "www.google.co.jp")
        assertNotEquals(null, google)
    }

    @Test
    fun typeとqueryが重複する項目をinsertすると例外送出() {
        try {
            val entry = IgnoredEntry(IgnoredEntryType.URL, "twitter.com")
            dao.insert(entry)
            fail("重複している項目がinsert成功してしまった")
        }
        catch (e: Exception) {
            Log.i("ok", "想定通りの例外送出")
        }
    }

    @Test
    fun データタイプが異なるがクエリは同一なアイテムは作成可能() {
        try {
            val entry = IgnoredEntry(IgnoredEntryType.TEXT, "twitter.com")
            dao.insert(entry)
        }
        catch (e: Exception) {
            fail("クエリだけ同一なアイテムをinsertできなかった")
        }
    }

    @Test
    fun 存在しない項目をdelete() {
        try {
            dao.delete(IgnoredEntry(IgnoredEntryType.URL, "invalid.com"))
        }
        catch (e: Exception) {
            fail("deleteが例外を送出")
        }
    }

    @Test
    fun 項目のdelete() {
        val prevSize = dao.getAllEntries().size
        val entry = IgnoredEntry(IgnoredEntryType.URL, "twitter.com")
        dao.delete(entry)
        assertEquals(null, dao.find(IgnoredEntryType.URL, "twitter.com"))
        assertEquals(prevSize - 1, dao.getAllEntries().size)
    }

    @Test
    fun converterが動いているか確認() {
        assertEquals(IgnoreTarget.ENTRY, dao.find(IgnoredEntryType.TEXT, "ハゲ")?.target)
    }
}
