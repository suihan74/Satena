package com.suihan74.satena.models

import android.content.Context
import android.util.Log
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.ignoredEntry.IgnoreTarget
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import java.lang.reflect.Type

/**************************************
 * version 2
 * DBに移行
 **************************************/
@Deprecated("DBに移行")
@SharedPreferencesKey(fileName = "ignored_entries", version = 2, latest = true)
enum class IgnoredEntriesKey(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {
    IGNORED_ENTRIES(typeInfo<List<IgnoredEntry>>(), emptyList<IgnoredEntry>())
}


////////////////////////////////////////////////////////////////////////////////
// previous versions
////////////////////////////////////////////////////////////////////////////////

/**************************************
 * version 1
 * URLの内容はドメイン以下（「http://」or「https://」を含める必要がない）
 **************************************/

//

/**************************************
 * version 0
 * URLの内容が「http://」or「https://」を含む完全なURL文字列
 **************************************/
@SharedPreferencesKey(fileName = "ignored_entries", version = 0)
enum class IgnoredEntriesKeyVersion0(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {
    IGNORED_ENTRIES(typeInfo<List<IgnoredEntry>>(), emptyList<IgnoredEntry>())
}


////////////////////////////////////////////////////////////////////////////////
// version migration
////////////////////////////////////////////////////////////////////////////////

object IgnoredEntriesKeyMigrator {
    fun check(context: Context) {
        val version = SafeSharedPreferences.version<IgnoredEntriesKey>(context)
        when (version) {
            0 -> {
                migrateFromVersion0(context)
                migrateFromVersion1(context)
            }

            1 -> migrateFromVersion1(context)
        }
    }

    private fun migrateFromVersion0(context: Context) {
        SafeSharedPreferences.migrate<IgnoredEntriesKeyVersion0, IgnoredEntriesKey>(context) { old, latest ->
            val entries = old.getObject<List<IgnoredEntry>>(IgnoredEntriesKeyVersion0.IGNORED_ENTRIES) ?: return@migrate
            val schemeRegex = Regex("""^https?://""")
            val predicateUrl : (IgnoredEntry)->Boolean = { it.type == IgnoredEntryType.URL }
            val texts = entries.filterNot(predicateUrl)
            val urls = entries
                .filter(predicateUrl)
                .map {
                    val url = it.query
                    val matchResult = schemeRegex.find(url)
                    val fixedUrl = if (matchResult == null) {
                        url
                    }
                    else {
                        url.substring(matchResult.range.last + 1)
                    }
                    IgnoredEntry(
                        type = it.type,
                        query = fixedUrl,
                        target = it.target,
                        createdAt = it.createdAt)
                }
                .distinctBy { it.query }

            val modifiedEntries = texts.plus(urls).sortedByDescending { it.createdAt }
            latest.edit {
                putObject(IgnoredEntriesKey.IGNORED_ENTRIES, modifiedEntries)
            }
        }
    }

    private fun migrateFromVersion1(context: Context) {
        val prefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context)
        val entries = prefs.getObject<List<IgnoredEntry>>(IgnoredEntriesKey.IGNORED_ENTRIES) ?: emptyList()
        val dao = SatenaApplication.instance.ignoredEntryDao
        dao.clearAllEntries()

        var success = true
        entries.forEach {
            try {
                dao.insert(
                    com.suihan74.satena.models.ignoredEntry.IgnoredEntry(
                        type = com.suihan74.satena.models.ignoredEntry.IgnoredEntryType.valueOf(it.type.name),
                        query = it.query,
                        target = IgnoreTarget.fromInt(it.target.int)
                    )
                )
            }
            catch (e: Exception) {
                Log.e("migrationError", e.message)
                success = false
            }
        }

        if (success) {
            prefs.edit {
                /* editを使用して設定バージョンを上書き */
            }
            Log.i("migration", "migration of IgnoredEntry is succeeded")
        }
    }
}
