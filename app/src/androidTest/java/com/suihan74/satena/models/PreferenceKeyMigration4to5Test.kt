package com.suihan74.satena.models

import androidx.core.content.edit
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.suihan74.utilities.SafeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class PreferenceKeyMigration4to5Test {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun 間隔が14分_境界15周辺() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS
        val rawPrefs = prefs.rawPrefs
        val old = 14L

        rawPrefs.edit {
            putInt("!VERSION", 4)
            putLong(key.name, old)
        }

        // チェック
        PreferenceKeyMigration.check(context)

        assertEquals(15L, prefs.getLong(key))
    }

    @Test
    fun 間隔が15分未満_非境界() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS
        val rawPrefs = prefs.rawPrefs
        val old = 5L

        rawPrefs.edit {
            putInt("!VERSION", 4)
            putLong(key.name, old)
        }

        // チェック
        PreferenceKeyMigration.check(context)

        assertEquals(15L, prefs.getLong(key))
    }

    @Test
    fun 間隔が1分_下限() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS
        val rawPrefs = prefs.rawPrefs
        val old = 1L

        rawPrefs.edit {
            putInt("!VERSION", 4)
            putLong(key.name, old)
        }

        // チェック
        PreferenceKeyMigration.check(context)

        assertEquals(15L, prefs.getLong(key))
    }

    @Test
    fun 間隔が15分_境界値() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS
        val rawPrefs = prefs.rawPrefs

        rawPrefs.edit {
            putInt("!VERSION", 4)
            putLong(key.name, 15L)
        }

        // チェック
        PreferenceKeyMigration.check(context)

        assertEquals(15L, prefs.getLong(key))
    }

    @Test
    fun 間隔が16分_境界15周辺() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS
        val rawPrefs = prefs.rawPrefs
        val expected = 16L

        rawPrefs.edit {
            putInt("!VERSION", 4)
            putLong(key.name, expected)
        }

        // チェック
        PreferenceKeyMigration.check(context)

        assertEquals(expected, prefs.getLong(key))
    }

    @Test
    fun 間隔が16分以上_非境界() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS
        val rawPrefs = prefs.rawPrefs
        val expected = 60L

        rawPrefs.edit {
            putInt("!VERSION", 4)
            putLong(key.name, expected)
        }

        // チェック
        PreferenceKeyMigration.check(context)

        assertEquals(expected, prefs.getLong(key))
    }

    @Test
    fun 間隔が180分_上限値() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS
        val rawPrefs = prefs.rawPrefs
        val expected = 180L

        rawPrefs.edit {
            putInt("!VERSION", 4)
            putLong(key.name, expected)
        }

        // チェック
        PreferenceKeyMigration.check(context)

        assertEquals(expected, prefs.getLong(key))
    }
}
