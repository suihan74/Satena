package com.suihan74.satena.scenes.entries2

import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.readEntry.ReadEntryCondition
import com.suihan74.satena.models.readEntry.ReadEntryDao
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.ZonedDateTime

class ReadEntriesRepository(
    private val dao : ReadEntryDao,
    private val prefs : SafeSharedPreferences<PreferenceKey>
) {
    /** ロード済みの既読エントリID */
    val readEntryIds : StateFlow<Set<Long>> = MutableStateFlow(emptySet())
    private val _readEntryIds
        get() = readEntryIds as MutableStateFlow<Set<Long>>

    private val readEntryIdsCache = HashSet<Long>()

    /** 既読マークを表示するか否か */
    val displaying : StateFlow<Boolean> = MutableStateFlow(
        prefs.getBoolean(PreferenceKey.ENTRY_DISPLAY_READ_MARK)
    )
    private val _displaying
        get() = displaying as MutableStateFlow<Boolean>

    /** 既読マークをつけるタイミング */
    val readEntryCondition = MutableStateFlow(
        ReadEntryCondition.fromInt(
            prefs.getInt(PreferenceKey.ENTRY_READ_MARK_CONDITION)
        )
    ).apply {
        onEach {
            prefs.edit {
                putInt(PreferenceKey.ENTRY_READ_MARK_CONDITION, it.int)
            }
        }.launchIn(SatenaApplication.instance.coroutineScope)
    }

    /** 期限切れのアイテムを最後に削除した日時 */
    private var lastDeletedOldItems : ZonedDateTime? = null

    // ------ //

    suspend fun insert(entry: Entry, timing: ReadEntryCondition) {
        if (readEntryCondition.value.contains(timing)) {
            dao.insert(entry)
            readEntryIdsCache.add(entry.id)
            if (displaying.value) {
                _readEntryIds.emit(readEntryIdsCache)
            }
        }
    }

    suspend fun load(entries: List<Entry>) {
        deleteOldItems()
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

    private suspend fun deleteOldItems() {
        runCatching {
            val now = ZonedDateTime.now()
            if (lastDeletedOldItems == null || now != lastDeletedOldItems) {
                val lifetime = prefs.getInt(PreferenceKey.ENTRY_READ_MARK_LIFETIME).toLong()
                if (lifetime > 0) {
                    dao.delete(now.minusDays(lifetime))
                    lastDeletedOldItems = now
                }
            }
        }
    }
}
