package com.suihan74.satena.models

import android.util.Log
import androidx.room.Room
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.suihan74.satena.models.userTag.insertRelation
import com.suihan74.satena.models.userTag.insertTag
import com.suihan74.satena.models.userTag.insertUser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("NonAsciiCharacters")
@RunWith(AndroidJUnit4ClassRunner::class)
class AppDatabaseTest {
    // Context of the app under test.
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val db =
        Room.inMemoryDatabaseBuilder(
            appContext, AppDatabase::class.java).build()
    private val dao = db.userTagDao()

    @Before
    fun データ初期化() {
        db.userTagDao().run {
            insertTag("test")
            insertTag("abc012")
            insertTag("テスト")

            insertUser("suihan")
            insertUser("suihan74")
            insertUser("すいはん")
        }
    }

    @Test
    fun タグ存在確認() {
        dao.run {
            assertNotEquals(null, findTag("test"))
            assertNotEquals(null, findTag("abc012"))
            assertNotEquals(null, findTag("テスト"))
        }
    }

    @Test
    fun 存在しないタグのfind結果はnull() {
        dao.run {
            assertEquals(null, findTag("テスト0"))
            assertEquals(null, findTag("a"))
            assertEquals(null, findTag(""))
            assertEquals(null, findTag("test "))
            assertEquals(null, findTag(" test"))
        }
    }

    @Test
    fun ユーザー存在確認() {
        dao.run {
            assertNotEquals(null, findUser("suihan"))
            assertNotEquals(null, findUser("suihan74"))
            assertNotEquals(null, findUser("すいはん"))
        }
    }

    @Test
    fun 存在しないユーザーのfind結果はnull() {
        dao.run {
            assertEquals(null, findUser("すいは0"))
            assertEquals(null, findUser("a"))
            assertEquals(null, findUser(""))
            assertEquals(null, findUser("suihan "))
            assertEquals(null, findUser(" suihan 7 4"))
        }
    }

    @Test
    fun タグ名変更() {
        val prevName = "変更前"
        val modifiedName = "変更後"

        dao.insertTag(prevName)
        val tag = dao.findTag(prevName)!!
        val modifiedTag = tag.copy(name = modifiedName)

        dao.updateTag(modifiedTag)

        assertEquals(null, dao.findTag(prevName))
        assertNotEquals(null, dao.findTag(modifiedName))

        dao.deleteTag(tag)
    }

    @Test
    fun 既存のタグ名に変更しようとする() {
        val prevName = "変更前"
        dao.insertTag(prevName)

        val tag = dao.findTag(prevName)!!
        val modifiedTag = tag.copy(name = "test")

        try {
            dao.updateTag(modifiedTag)
            fail("重複したタグ名更新を許している")
        }
        catch (e: Exception) {
            Log.i("DBtest", "valid exception throwing")
        }

        dao.deleteTag(modifiedTag)
    }

    @Test
    fun ユーザーをタグ付け() {
        val user = dao.findUser("suihan")!!
        val tag1 = dao.findTag("test")!!
        val tag2 = dao.findTag("テスト")!!

        dao.insertRelation(tag1, user)
        dao.insertRelation(tag2, user)

        try {
            dao.insertRelation(tag1, user)
            fail("重複したタグ付けを許している")
        }
        catch (e: Exception) {
            Log.i("DBtest", "valid exception throwing")
        }

        assertEquals(2, dao.getUserAndTags(user.name)?.tags?.size)
    }

    @Test
    fun まだ使われているタグを削除() {
        dao.insertTag("削除する")
        val user = dao.findUser("suihan")!!
        val tag = dao.findTag("削除する")!!

        dao.insertRelation(tag, user)
        dao.deleteTag(tag)

        assertEquals(0, dao.getUserAndTags(user.name)?.tags?.size)
    }
}
