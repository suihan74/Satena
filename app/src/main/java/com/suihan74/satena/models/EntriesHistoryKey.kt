package com.suihan74.satena.models

import android.content.Context
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.readEntry.ReadEntryCondition
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Type

/**
 * エントリ表示履歴を保存
 */
@SharedPreferencesKey(fileName = "entries_history", version = 1, latest = true)
enum class EntriesHistoryKey (
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {
    /** エントリ履歴 */
    ENTRIES(typeInfo<List<Entry>>(), emptyList<Entry>()),
    /** 最大保存数 */
    MAX_SIZE(typeInfo<Int>(), 20);

    companion object {
        /** 最大保存数の下限値 */
        const val MAX_SIZE_LOWER_BOUND = 1
        /** 最大保存数の上限値 */
        const val MAX_SIZE_UPPER_BOUND = 100
    }
}

// TODO: entry画面用のVM/Repositoryにて行うようにする
/** エントリを表示履歴に追加/更新する */
fun Entry.saveHistory(context: Context) {
    val prefs = SafeSharedPreferences.create<EntriesHistoryKey>(context)
    try {
        val entries = prefs.get<List<Entry>>(EntriesHistoryKey.ENTRIES)
        val maxSize = prefs.getInt(EntriesHistoryKey.MAX_SIZE)

        val existedPosition = entries.indexOfFirst { it.url == this.url }
        val modifiedEntries =
            when {
                existedPosition < 0 -> entries.plus(this)
                else -> {
                    entries.filterNot { it.url == this.url }
                        .plus(this)
                }
            }
                .takeLast(maxSize)

        if (modifiedEntries != entries) {
            prefs.edit {
                put(EntriesHistoryKey.ENTRIES, modifiedEntries)
            }
        }
    }
    catch (e: Throwable) {
        prefs.edit {
            put(EntriesHistoryKey.ENTRIES, emptyList<Entry>())
        }
    }
}

////////////////////////////////////////////////////////////////////////////////
// version migration
////////////////////////////////////////////////////////////////////////////////

@Suppress("deprecation")
object EntriesHistoryKeyMigration {
    fun check(context: Context) {
        while (true) {
            when (SafeSharedPreferences.version<EntriesHistoryKey>(context)) {
                0 -> migrateFromVersion0(context)

                else -> break
            }
        }
    }

    /** v0 -> v1: 既存の既読エントリ情報を既読マークに反映する */
    private fun migrateFromVersion0(context: Context) = runBlocking {
        val repo = SatenaApplication.instance.readEntriesRepository
        val prefs = SafeSharedPreferences.create<EntriesHistoryKey>(context)
        prefs.getObject<List<Entry>>(EntriesHistoryKey.ENTRIES)?.forEach { entry ->
            repo.insert(entry, ReadEntryCondition.BOOKMARKS_SHOWN)
        }
        prefs.edit { /* バージョン情報の更新に必要 */ }
    }
}
