package com.suihan74.satena.scenes.browser.history

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.suihan74.satena.models.browser.BrowserDao
import com.suihan74.satena.models.browser.History
import com.suihan74.utilities.extensions.faviconUrl
import com.suihan74.utilities.extensions.limitedPlus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class HistoryRepository(
    private val dao: BrowserDao
) {

    /** 閲覧履歴 */
    val histories by lazy {
        MutableLiveData<List<History>>(emptyList())
    }

    /** ロード済みの全閲覧履歴データのキャッシュ */
    private var historiesCache = ArrayList<History>()
    private val historiesCacheLock by lazy { Mutex() }

    /** 検索ワード */
    val keyword by lazy {
        MutableLiveData<String?>(null)
    }

    // ------ //

    /** 履歴を追加する */
    suspend fun insertHistory(
        url: String,
        title: String,
        faviconUrl: String? = null
    ) = withContext(Dispatchers.IO) {
        val existed = dao.getHistory(url)

        val history = History(
            url = Uri.decode(url),
            title = title,
            faviconUrl = faviconUrl ?: Uri.parse(url).faviconUrl,
            lastVisited = LocalDateTime.now(),
            visitTimes = existed?.visitTimes?.limitedPlus(1L) ?: 1L
        )
        dao.insertHistory(history)

        historiesCacheLock.withLock {
            historiesCache.removeAll { it.url == history.url }
            historiesCache.add(history)
        }

        updateHistoriesLiveData()
    }

    /** 履歴を削除する */
    suspend fun deleteHistory(history: History) = withContext(Dispatchers.IO) {
        dao.deleteHistory(history)

        historiesCacheLock.withLock {
            historiesCache.removeAll { it.url == history.url }
        }
        updateHistoriesLiveData()
    }

    /** 履歴リストを更新 */
    suspend fun loadHistories() = withContext(Dispatchers.IO) {
        historiesCacheLock.withLock {
            historiesCache.clear()
            historiesCache.addAll(
                dao.getRecentHistories()
            )
        }

        updateHistoriesLiveData()
    }

    /** 履歴をすべて削除 */
    suspend fun clearHistories() = withContext(Dispatchers.IO) {
        dao.clearHistory()
        loadHistories()
    }

    /** 指定した日付の履歴をすべて削除 */
    suspend fun clearHistories(date: LocalDate) = withContext(Dispatchers.IO) {
        val start = date.atTime(0, 0)
        val end = date.plusDays(1L).atTime(0, 0)
        dao.deleteHistory(start, end)

        historiesCacheLock.withLock {
            historiesCache.removeAll { h ->
                h.lastVisited.toLocalDate() == date
            }
        }

        updateHistoriesLiveData()
    }

    /** 履歴リストの続きを取得 */
    suspend fun loadAdditional() = withContext(Dispatchers.IO) {
        historiesCacheLock.withLock {
            val additional = dao.getRecentHistories(offset = historiesCache.size)

            historiesCache.addAll(additional)
            historiesCache.sortBy { it.lastVisited }
        }

        updateHistoriesLiveData()
    }

    /** 表示用の履歴リストを更新する */
    suspend fun updateHistoriesLiveData() = withContext(Dispatchers.IO) {
        val locale = Locale.JAPANESE
        val keyword = keyword.value?.toLowerCase(locale)
        historiesCacheLock.withLock {
            if (keyword.isNullOrBlank()) {
                histories.postValue(historiesCache)
            }
            else {
                val dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu/MM/dd hh:mm")
                val keywords = keyword.split(Regex("""\s+"""))
                val list = historiesCache.filter {
                    keywords.all { k ->
                        it.title.toLowerCase(locale).contains(k)
                                || it.url.toLowerCase(locale).contains(k)
                                || it.lastVisited.format(dateTimeFormatter).contains(k)
                    }
                }
                histories.postValue(list)
            }
        }
    }
}
