package com.suihan74.satena.scenes.preferences.ignored

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.NotFoundException
import com.suihan74.satena.models.ignoredEntry.IgnoreTarget
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import com.suihan74.utilities.exceptions.AlreadyExistedException
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.updateFirstOrPlus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class IgnoredEntriesRepository(
    private val dao: IgnoredEntryDao
) {
    private var _ignoredEntriesForBookmarks : List<IgnoredEntry> = emptyList()
    /** ブクマ用の非表示ワードリスト */
    val ignoredEntriesForBookmarks: List<IgnoredEntry> get() = _ignoredEntriesForBookmarks

    // ------ //

    private val _ignoredEntriesForEntries = MutableLiveData<List<IgnoredEntry>>()
    /** エントリミュート用の非表示設定リスト */
    val ignoredEntriesForEntries : LiveData<List<IgnoredEntry>> = _ignoredEntriesForEntries

    // ------ //

    private val ignoredEntriesCacheLock by lazy { Mutex() }

    private val _ignoredEntries = MutableLiveData<List<IgnoredEntry>>()
    val ignoredEntries: LiveData<List<IgnoredEntry>> = _ignoredEntries

    // ------ //

    /** 全ての非表示設定をロードする */
    suspend fun loadAllIgnoredEntries(forceUpdate: Boolean = false) = withContext(Dispatchers.IO) {
        ignoredEntriesCacheLock.withLock {
            if (forceUpdate || ignoredEntries.value.isNullOrEmpty()) {
                val allEntries = dao.getAllEntries()
                _ignoredEntries.postValue(allEntries)

                val forEntries = ArrayList<IgnoredEntry>()
                val forBookmarks = ArrayList<IgnoredEntry>()
                for (entry in allEntries) {
                    if (entry.target contains IgnoreTarget.ENTRY) {
                        forEntries.add(entry)
                    }
                    if (entry.target contains IgnoreTarget.BOOKMARK){
                        forBookmarks.add(entry)
                    }
                }
                _ignoredEntriesForEntries.postValue(forEntries)
                _ignoredEntriesForBookmarks = forBookmarks
            }
        }
    }

    /** NGエントリをロードする */
    suspend fun loadIgnoredEntriesForEntries(forceUpdate: Boolean = false) = withContext(Dispatchers.IO) {
        ignoredEntriesCacheLock.withLock {
            if (_ignoredEntriesForEntries.value.isNullOrEmpty() || forceUpdate) {
                _ignoredEntriesForEntries.postValue(dao.getEntriesForEntries())
            }
        }
    }

    /** NGワードをロードする */
    suspend fun loadIgnoredWordsForBookmarks(forceUpdate: Boolean = false) = withContext(Dispatchers.IO) {
        ignoredEntriesCacheLock.withLock {
            if (_ignoredEntriesForBookmarks.isEmpty() || forceUpdate) {
                _ignoredEntriesForBookmarks = dao.getEntriesForBookmarks()
            }
        }
    }

    /**
     * 項目を追加する
     *
     * @throws AlreadyExistedException
     */
    suspend fun addIgnoredEntry(entry: IgnoredEntry) = withContext(Dispatchers.IO) {
        try {
            ignoredEntriesCacheLock.withLock {
                dao.insert(entry)
                dao.find(entry.type, entry.query)?.let {
                    if (it.target contains IgnoreTarget.BOOKMARK) {
                        _ignoredEntriesForBookmarks = _ignoredEntriesForBookmarks.plus(it)
                    }

                    if (it.target contains IgnoreTarget.ENTRY) {
                        val existed = _ignoredEntriesForEntries.value.orEmpty()
                        _ignoredEntriesForEntries.postValue(existed.plus(it))
                    }

                    _ignoredEntries.postValue(ignoredEntries.value.orEmpty().plus(it))
                }
            }
        }
        catch (e: Throwable) {
            throw AlreadyExistedException()
        }
    }

    /**
     * 項目を削除する
     *
     * @throws NotFoundException
     */
    suspend fun deleteIgnoredEntry(entry: IgnoredEntry) = withContext(Dispatchers.IO) {
        try {
            ignoredEntriesCacheLock.withLock {
                dao.delete(entry)

                if (entry.target contains IgnoreTarget.ENTRY) {
                    val existed = _ignoredEntriesForEntries.value.orEmpty()
                    _ignoredEntriesForEntries.postValue(
                        existed.filterNot { it.id == entry.id }
                    )
                }

                if (entry.target contains IgnoreTarget.BOOKMARK) {
                    _ignoredEntriesForBookmarks = _ignoredEntriesForBookmarks.minus(entry)
                }

                _ignoredEntries.postValue(ignoredEntries.value.orEmpty().minus(entry))
            }
        }
        catch (e: Throwable) {
            throw NotFoundException()
        }
    }

    /**
     * 項目の内容を更新する
     *
     * @throws TaskFailureException
     */
    suspend fun updateIgnoredEntry(entry: IgnoredEntry) = withContext(Dispatchers.IO) {
        try {
            ignoredEntriesCacheLock.withLock {
                dao.update(entry)
                dao.find(entry.type, entry.query)?.also { newItem ->
                    val entries = ignoredEntries.value.orEmpty()
                    val existedIdx = entries.indexOfFirst { it.id == newItem.id }
                    if (existedIdx < 0) return@withLock
                    val existed = entries[existedIdx]

                    if (newItem.target contains IgnoreTarget.BOOKMARK) {
                        _ignoredEntriesForBookmarks = _ignoredEntriesForBookmarks.updateFirstOrPlus(newItem) {
                            existed.target contains IgnoreTarget.BOOKMARK && it == existed
                        }
                    }

                    if (newItem.target contains IgnoreTarget.ENTRY) {
                        val prevList = _ignoredEntriesForEntries.value.orEmpty()
                        _ignoredEntriesForEntries.postValue(prevList.updateFirstOrPlus(newItem) {
                            existed.target contains IgnoreTarget.ENTRY && it.id == newItem.id
                        })
                    }

                    _ignoredEntries.postValue(buildList {
                        if (existedIdx > 0) {
                            addAll(entries.subList(0, existedIdx))
                        }
                        add(entry)
                        if (existedIdx < entries.lastIndex) {
                            addAll(entries.subList(existedIdx + 1, entries.size))
                        }
                    })
                }
            }
        }
        catch (e: Throwable) {
            throw NotFoundException()
        }
    }
}
