package com.suihan74.satena.scenes.preferences.ignored

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.NotFoundException
import com.suihan74.satena.models.ignoredEntry.IgnoreTarget
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import com.suihan74.utilities.exceptions.AlreadyExistedException
import com.suihan74.utilities.exceptions.TaskFailureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** ブクマ画面で使用する機能 */
interface IgnoredEntriesRepositoryForBookmarks {
    /** ブクマ用の非表示ワードリスト */
    val ignoredWordsForBookmarks : List<String>

    /** ブクマ画面用のNGワード設定をロードする */
    suspend fun loadIgnoredWordsForBookmarks()
}

// ------ //

/** エントリ画面で使用する機能 */
interface IgnoredEntriesRepositoryForEntries {
    /** エントリ用の非表示設定リスト */
    val ignoredEntriesForEntries : List<IgnoredEntry>

    /** エントリ画面用のNG設定をロードする */
    suspend fun loadIgnoredEntriesForEntries()

    /** 項目を追加する */
    @Throws(AlreadyExistedException::class)
    suspend fun addIgnoredEntry(entry: IgnoredEntry)
}

// ------ //

class IgnoredEntriesRepository(
    private val dao: IgnoredEntryDao
) :
    IgnoredEntriesRepositoryForBookmarks,
    IgnoredEntriesRepositoryForEntries
{
    /** ブクマ用の非表示ワードリスト */
    private var _ignoredWordsForBookmarks : List<String> = emptyList()

    override val ignoredWordsForBookmarks: List<String>
        get() = _ignoredWordsForBookmarks

    /** NGワードをロードする */
    override suspend fun loadIgnoredWordsForBookmarks() = withContext(Dispatchers.IO) {
        _ignoredWordsForBookmarks = dao.getEntriesForBookmarks()
            .map { it.query }
    }

    // ------ //

    private var _ignoredEntriesForEntries : List<IgnoredEntry> = emptyList()

    override val ignoredEntriesForEntries : List<IgnoredEntry>
        get() = _ignoredEntriesForEntries

    /** NG設定をロードする */
    override suspend fun loadIgnoredEntriesForEntries() {
        _ignoredEntriesForEntries = dao.getEntriesForEntries()
    }

    // ------ //

    private var ignoredEntriesCache : ArrayList<IgnoredEntry>? = null
    private val ignoredEntriesCacheLock by lazy { Mutex() }

    private val _ignoredEntries by lazy {
        MutableLiveData<List<IgnoredEntry>>()
    }
    val ignoreEntries: LiveData<List<IgnoredEntry>>
        get() = _ignoredEntries

    /** 全ての非表示設定をロードする */
    suspend fun loadAllIgnoredEntries(
        forceUpdate: Boolean = false
    ) = withContext(Dispatchers.IO) {
        ignoredEntriesCacheLock.withLock {
            if (forceUpdate || ignoredEntriesCache == null) {
                ignoredEntriesCache = ArrayList(dao.getAllEntries())
                _ignoredEntries.postValue(ignoredEntriesCache)
            }
        }
    }

    /** 項目を追加する */
    @Throws(AlreadyExistedException::class)
    override suspend fun addIgnoredEntry(
        entry: IgnoredEntry
    ) = withContext(Dispatchers.IO) {
        try {
            ignoredEntriesCacheLock.withLock {
                dao.insert(entry)
                dao.find(entry.type, entry.query)?.also {
                    ignoredEntriesCache?.add(it)
                    if (it.target contains IgnoreTarget.BOOKMARK) {
                        _ignoredWordsForBookmarks = _ignoredWordsForBookmarks.plus(it.query)
                    }
                    if (it.target contains IgnoreTarget.ENTRY) {
                        _ignoredEntriesForEntries = _ignoredEntriesForEntries.plus(it)
                    }
                }
                _ignoredEntries.postValue(ignoredEntriesCache)
            }
        }
        catch (e: Throwable) {
            throw AlreadyExistedException()
        }
    }

    /** 項目を削除する */
    @Throws(NotFoundException::class)
    suspend fun deleteIgnoredEntry(
        entry: IgnoredEntry
    ) = withContext(Dispatchers.IO) {
        try {
            ignoredEntriesCacheLock.withLock {
                dao.delete(entry)
                ignoredEntriesCache?.remove(entry)

                if (entry.target contains IgnoreTarget.ENTRY) {
                    _ignoredEntriesForEntries = _ignoredEntriesForEntries.filterNot { it.id == entry.id }
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

    /** 項目の内容を更新する */
    @Throws(TaskFailureException::class)
    suspend fun updateIgnoredEntry(
        modified: IgnoredEntry
    ) = withContext(Dispatchers.IO) {
        try {
            ignoredEntriesCacheLock.withLock {
                dao.update(modified)
                val idx = ignoredEntriesCache?.indexOfFirst { it.id == modified.id } ?: -1
                if (idx >= 0) {
                    ignoredEntriesCache?.removeAt(idx)
                    ignoredEntriesCache?.add(idx, modified)
                }
                _ignoredEntries.postValue(ignoredEntriesCache)
            }
        }
        catch (e: Throwable) {
            throw TaskFailureException("failed to update the entry")
        }
    }
}
