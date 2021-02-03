package com.suihan74.satena.models

import android.content.Context
import com.suihan74.hatenaLib.Entry
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import java.lang.reflect.Type

/**************************************
 * version 1
 **************************************/

/**
 * エントリ表示履歴を保存
 */
@SharedPreferencesKey(fileName = "entries_history", version = 0, latest = true)
enum class EntriesHistoryKey (
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {
    /** エントリ履歴 */
    ENTRIES(typeInfo<List<Entry>>(), emptyList<Entry>()),
    /** 最大保存数 */
    MAX_SIZE(typeInfo<Int>(), 20),
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
