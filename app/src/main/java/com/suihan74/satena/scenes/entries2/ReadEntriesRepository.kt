package com.suihan74.satena.scenes.entries2

import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.readEntry.ReadEntryDao
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReadEntriesRepository(
    private val dao : ReadEntryDao,
    private val prefs : SafeSharedPreferences<PreferenceKey>
) {
    /** ロード済みの既読エントリID */
    val readEntryIds : StateFlow<Set<Long>> = MutableStateFlow(emptySet())
    private val _readEntryIds
        get() = readEntryIds as MutableStateFlow<Set<Long>>

    private val readEntryIdsCache = HashSet<Long>()

    val displaying : StateFlow<Boolean> = MutableStateFlow(
        prefs.getBoolean(PreferenceKey.ENTRY_DISPLAY_READ_MARK)
    )
    private val _displaying
        get() = displaying as MutableStateFlow<Boolean>

    // ------ //

    suspend fun insert(entry: Entry) {
        dao.insert(entry)
        readEntryIdsCache.add(entry.id)
        if (displaying.value) {
            _readEntryIds.emit(readEntryIdsCache)
        }
    }

    suspend fun load(entries: List<Entry>) {
        val ids = entries.mapNotNull {
            when (it.id) {
                0L -> null
                else -> it.id
            }
        }
        val foundIds = dao.find(ids)
        readEntryIdsCache.addAll(foundIds.map { it.eid })
        if (displaying.value) {
            _readEntryIds.emit(readEntryIdsCache)
        }
    }

    suspend fun setDisplaying(enabled: Boolean) {
        if (enabled == displaying.value) return
        prefs.edit {
            putBoolean(PreferenceKey.ENTRY_DISPLAY_READ_MARK, enabled)
        }
        _displaying.emit(enabled)
        _readEntryIds.emit(
            if (enabled) readEntryIdsCache
            else emptySet()
        )
    }
}
