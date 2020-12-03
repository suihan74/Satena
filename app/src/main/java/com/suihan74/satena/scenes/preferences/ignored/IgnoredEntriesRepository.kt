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
    /** ブクマ用の非表示ワードリスト */
    val ignoredWordsForBookmarks: List<String>
        get() = _ignoredWordsForBookmarks
    private var _ignoredWordsForBookmarks : List<String> = emptyList()

    // ------ //

    val ignoredEntriesForEntries : LiveData<List<IgnoredEntry>>
        get() = _ignoredEntriesForEntries
    private var _ignoredEntriesForEntries = MutableLiveData<List<IgnoredEntry>>()

    // ------ //

    private var ignoredEntriesCache : ArrayList<IgnoredEntry> = ArrayList()
    private val ignoredEntriesCacheLock by lazy { Mutex() }

    val ignoredEntries: LiveData<List<IgnoredEntry>>
        get() = _ignoredEntries
    private val _ignoredEntries = MutableLiveData<List<IgnoredEntry>>()

    // ------ //

    /** 全ての非表示設定をロードする */
    suspend fun loadAllIgnoredEntries(
        forceUpdate: Boolean = false
    ) = withContext(Dispatchers.IO) {
        ignoredEntriesCacheLock.withLock {
            if (forceUpdate || ignoredEntriesCache.isEmpty()) {
                val allEntries = dao.getAllEntries()
                ignoredEntriesCache.clear()
                ignoredEntriesCache.addAll(allEntries)
                _ignoredEntries.postValue(ignoredEntriesCache)

                _ignoredEntriesForEntries.postValue(allEntries.filter {
                    it.target contains IgnoreTarget.ENTRY
                })

                _ignoredWordsForBookmarks = allEntries.filter {
                    it.target contains IgnoreTarget.BOOKMARK
                }.map {
                    it.query
                }
            }
        }
    }

    /** NGエントリをロードする */
    suspend fun loadIgnoredEntriesForEntries(
        forceUpdate: Boolean = false
    ) = withContext(Dispatchers.IO) {
        ignoredEntriesCacheLock.withLock {
            if (_ignoredEntriesForEntries.value.isNullOrEmpty() || forceUpdate) {
                _ignoredEntriesForEntries.postValue(dao.getEntriesForEntries())
            }
        }
    }

    /** NGワードをロードする */
    suspend fun loadIgnoredWordsForBookmarks(
        forceUpdate: Boolean = false
    ) = withContext(Dispatchers.IO) {
        ignoredEntriesCacheLock.withLock {
            if (_ignoredWordsForBookmarks.isNullOrEmpty() || forceUpdate) {
                _ignoredWordsForBookmarks = dao.getEntriesForBookmarks()
                    .map { it.query }
            }
        }
    }

    /**
     * 項目を追加する
     *
     * @throws AlreadyExistedException
     */
    suspend fun addIgnoredEntry(
        entry: IgnoredEntry
    ) = withContext(Dispatchers.IO) {
        try {
            ignoredEntriesCacheLock.withLock {
                dao.insert(entry)
                dao.find(entry.type, entry.query)?.also {
                    ignoredEntriesCache.add(it)

                    if (it.target contains IgnoreTarget.BOOKMARK) {
                        _ignoredWordsForBookmarks = _ignoredWordsForBookmarks.plus(it.query)
                    }

                    if (it.target contains IgnoreTarget.ENTRY) {
                        val existed = _ignoredEntriesForEntries.value.orEmpty()
                        _ignoredEntriesForEntries.postValue(existed.plus(it))
                    }
                }

                _ignoredEntries.postValue(ignoredEntriesCache)
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
    suspend fun deleteIgnoredEntry(
        entry: IgnoredEntry
    ) = withContext(Dispatchers.IO) {
        try {
            ignoredEntriesCacheLock.withLock {
                dao.delete(entry)
                ignoredEntriesCache.remove(entry)

                if (entry.target contains IgnoreTarget.ENTRY) {
                    val existed = _ignoredEntriesForEntries.value.orEmpty()
                    _ignoredEntriesForEntries.postValue(
                        existed.filterNot { it.id == entry.id }
                    )
                }

                if (entry.target contains IgnoreTarget.BOOKMARK) {
                    _ignoredWordsForBookmarks = _ignoredWordsForBookmarks.filterNot { it == entry.query }
                }

                _ignoredEntries.postValue(ignoredEntriesCache)
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
    suspend fun updateIgnoredEntry(
        entry: IgnoredEntry
    ) = withContext(Dispatchers.IO) {
        try {
            ignoredEntriesCacheLock.withLock {
                dao.update(entry)
                dao.find(entry.type, entry.query)?.also { newItem ->
                    val existedIdx = ignoredEntriesCache.indexOfFirst { it.id == newItem.id }
                    if (existedIdx < 0) return@withLock
                    val existed = ignoredEntriesCache[existedIdx]
                    ignoredEntriesCache[existedIdx] = newItem

                    if (newItem.target contains IgnoreTarget.BOOKMARK) {
                        _ignoredWordsForBookmarks = _ignoredWordsForBookmarks.updateFirstOrPlus(newItem.query) {
                            existed.target contains IgnoreTarget.BOOKMARK && it == existed.query
                        }
                    }

                    if (newItem.target contains IgnoreTarget.ENTRY) {
                        val prevList = _ignoredEntriesForEntries.value.orEmpty()
                        _ignoredEntriesForEntries.postValue(prevList.updateFirstOrPlus(newItem) {
                            existed.target contains IgnoreTarget.ENTRY && it.id == newItem.id
                        })
                    }
                }

                _ignoredEntries.postValue(ignoredEntriesCache)
            }
        }
        catch (e: Throwable) {
            throw NotFoundException()
        }
    }
}
