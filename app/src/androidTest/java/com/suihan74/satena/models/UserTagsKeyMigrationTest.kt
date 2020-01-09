package com.suihan74.satena.models

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.suihan74.satena.models.userTag.UserTagDao
import com.suihan74.satena.models.userTag.clearAll
import com.suihan74.satena.models.userTag.findRelation
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

@Suppress("NonAsciiCharacters", "SpellCheckingInspection", "DEPRECATION")
class UserTagsKeyMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Test
    fun sharedPreferencesからRoomDBへの移行() {
        val prefs = SafeSharedPreferences.create<UserTagsKey>(context)
        val container = prefs.get<UserTagsContainer>(UserTagsKey.CONTAINER)
        val dao = db.userTagDao()

        // --- テストデータを作成 --- //
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
        // ------ //

        // --- 移行 --- //
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

        // ------ //
    }

    private suspend fun Method.invokeSuspend(obj: Any, vararg args: Any?) : Any? =
        suspendCoroutineUninterceptedOrReturn { cont ->
            invoke(obj, *args, cont)
        }
}
