package com.suihan74.satena.scenes.entries2

import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.readEntry.ReadEntryBehavior
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
    private val _readEntryIds = readEntryIds as MutableStateFlow<Set<Long>>

    private val readEntryIdsCache = HashSet<Long>()

    /** 既読マークを表示するか否か */
    val displaying : StateFlow<Boolean> = MutableStateFlow(
        prefs.getInt(PreferenceKey.ENTRY_READ_BEHAVIOR) != ReadEntryBehavior.NONE.int
    )
    private val _displaying = displaying as MutableStateFlow<Boolean>

    /** 既読エントリの振舞い */
    val readEntryBehavior = MutableStateFlow(
        ReadEntryBehavior.fromInt(
            prefs.getInt(PreferenceKey.ENTRY_READ_BEHAVIOR)
        )
    ).apply {
        onEach {
            prefs.edit {
                putInt(PreferenceKey.ENTRY_READ_BEHAVIOR, it.int)
            }
            setBehavior(it)
        }.launchIn(SatenaApplication.instance.coroutineScope)
    }

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
                _readEntryIds.emit(readEntryIds.value.plus(entry.id))
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
        val foundIds = dao.find(ids).map { it.eid }
        readEntryIdsCache.addAll(foundIds)
        if (displaying.value) {
            _readEntryIds.emit(readEntryIds.value.plus(foundIds))
        }
    }

    suspend fun delete(entry: Entry) {
        dao.delete(entry)
        readEntryIdsCache.remove(entry.id)
        if (displaying.value) {
            _readEntryIds.emit(readEntryIds.value.minus(entry.id))
        }
    }

    private suspend fun setBehavior(behavior: ReadEntryBehavior) {
        val enabled = behavior != ReadEntryBehavior.NONE
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
