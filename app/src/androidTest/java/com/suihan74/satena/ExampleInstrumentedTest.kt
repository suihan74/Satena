package com.suihan74.satena

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Type

@SharedPreferencesKey(fileName = "test", version = 11)
enum class TestKeyOld(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {

    INT(typeInfo<Int>(), 0),
    STRING(typeInfo<String>(), null),
    JSON(typeInfo<ArrayList<Int>>(), null),

    EMPTY_KEY(typeInfo<String>(), ""),

    FOO_FLAG(typeInfo<Boolean>(), false),
    BAR_STR(typeInfo<String>(), ""),
    BAZ_OBJ(typeInfo<List<Int>>(), null),
}

@SharedPreferencesKey(fileName = "test2", version = 12, latest = true)
enum class TestKey(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {

    INT(typeInfo<Int>(), 0),
    STRING(typeInfo<String>(), null),
    JSON(typeInfo<ArrayList<Int>>(), null),

    EMPTY_KEY(typeInfo<String>(), ""),

    FOO_FLAG(typeInfo<Boolean>(), false),
    BAR_STR(typeInfo<String>(), ""),
    BAZ_OBJ(typeInfo<List<Int>>(), null),
}

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class ExampleInstrumentedTest {
    // Context of the app under test.
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    init {
        // キーバージョン移行テスト
        SafeSharedPreferences.migrate<TestKeyOld, TestKey>(appContext) { old, latest -> }
    }

    @Test
    fun useAppContext() {
        // test for the package name
        assertEquals("com.suihan74.satena", appContext.packageName)
    }

    @Test
    fun safeSharedPreferencesWithIntValue() {
        val ssp = SafeSharedPreferences.create<TestKey>(appContext)
        val src = 1234
        ssp.edit {
            putInt(TestKey.INT, src)
        }
        val dest = ssp.get<Int>(TestKey.INT)
        assertEquals(src, dest)
    }

    @Test
    fun safeSharedPreferencesWithJsonValue() {
        val ssp = SafeSharedPreferences.create<TestKey>(appContext)
        val src = arrayListOf(1, 2, 3, 4, 5)
        ssp.edit {
            put(TestKey.JSON, src)
        }
        val dest = ssp.get<ArrayList<Int>>(TestKey.JSON)
        assertEquals(src, dest)
    }

    @Test
    fun safeSharedPreferencesWithStringValue() {
        val ssp = SafeSharedPreferences.create<TestKey>(appContext)
        val src = "hello world"
        ssp.edit {
            putString(TestKey.STRING, src)
        }
        val dest = ssp.get<String>(TestKey.STRING)
        assertEquals(src, dest)
    }

    @Test
    fun safeSharedPreferencesWithGenericGetter() {
        val ssp = SafeSharedPreferences.create<TestKey>(appContext)
        val src = "hello world"
        ssp.edit {
            put(TestKey.STRING, src)
        }

        val dest = ssp.get<String>(TestKey.STRING)
        assertEquals("hello world", dest)
    }

    @Test
    fun typeInfoTest() {
        assertEquals(typeInfo<Int>(), typeInfo<Int>())
        assertNotEquals(typeInfo<Any>(), typeInfo<Int>())
        assertEquals(typeInfo<List<Int>>(), typeInfo<List<Int>>())
        assertNotEquals(typeInfo<Int>(), typeInfo<List<Int>>())
        assertEquals(typeInfo<List<Map<String, Int>>>(), typeInfo<List<Map<String, Int>>>())
        assertNotEquals(typeInfo<List<Map<String, Int>>>(), typeInfo<List<Map<String, Long>>>())
    }


    @Test
    fun test_foo() {
        val ssp = SafeSharedPreferences.create<TestKey>(appContext)
        val src = true
        ssp.edit {
            put(TestKey.FOO_FLAG, src)
        }
        val dest = ssp.getBoolean(TestKey.FOO_FLAG)
        assertEquals(src, dest)
    }

    @Test
    fun test_bar() {
        val ssp = SafeSharedPreferences.create<TestKey>(appContext)
        val src = "hello world"
        ssp.edit {
            put(TestKey.BAR_STR, src)
        }
        val dest = ssp.getString(TestKey.BAR_STR)
        assertEquals(src, dest)
    }

    @Test
    fun test_baz() {
        val ssp = SafeSharedPreferences.create<TestKey>(appContext)
        val src = arrayListOf(1, 2, 3, 4, 5)
        ssp.edit {
            put(TestKey.BAZ_OBJ, src)
        }
        val dest = ssp.get<List<Int>>(TestKey.BAZ_OBJ)
        assertEquals(src, dest)
    }
}
