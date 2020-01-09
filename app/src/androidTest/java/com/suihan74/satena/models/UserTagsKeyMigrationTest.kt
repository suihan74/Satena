package com.suihan74.satena.models

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.suihan74.satena.models.userTag.UserTagDao
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.invokeSuspend
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.Continuation

@Suppress("NonAsciiCharacters", "SpellCheckingInspection", "DEPRECATION")
class UserTagsKeyMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    /** テストデータの作成 */
    @Before
    fun initialize() {
        val prefs = SafeSharedPreferences.create<UserTagsKey>(context)
        val container = prefs.get<UserTagsContainer>(UserTagsKey.CONTAINER)
        val dao = db.userTagDao()

        container.run {
            val userHoge = addUser("hoge")
            val userFuga = addUser("fuga")
            val tagFoo = addTag("FOO")
            val tagBar = addTag("BAR")

            tagUser(userHoge, tagFoo)
            tagUser(userFuga, tagBar)
            tagUser(userHoge, tagBar)
        }
        prefs.edit {
            put(UserTagsKey.CONTAINER, container)
        }
        dao.clearAll()
    }

    @Test
    fun sharedPreferencesからRoomDBへの移行() {
        val prefs = SafeSharedPreferences.create<UserTagsKey>(context)
        val dao = db.userTagDao()

        // テスト対象メソッドを取得
        val migrationMethod = UserTagsKeyMigration::class.java.getDeclaredMethod(
            "migrateFromVersion0",
            SafeSharedPreferences::class.java,
            UserTagDao::class.java,
            Continuation::class.java
        ).apply {
            isAccessible = true
        }

        runBlocking {
            migrationMethod.invokeSuspend(UserTagsKeyMigration, prefs, dao)
        }

        val userHoge = dao.findUser("hoge")
        val userFuga = dao.findUser("fuga")
        val tagFoo = dao.findTag("FOO")
        val tagBar = dao.findTag("BAR")
        assertNotNull(userHoge)
        assertNotNull(userFuga)
        assertNotNull(tagFoo)
        assertNotNull(tagBar)

        val relFooHoge = dao.findRelation(tagFoo!!, userHoge!!)
        val relBarHoge = dao.findRelation(tagBar!!, userHoge)
        val relBarFuga = dao.findRelation(tagBar, userFuga!!)
        assertNotNull(relFooHoge)
        assertNotNull(relBarHoge)
        assertNotNull(relBarFuga)
    }
}
