package com.suihan74.satena.scenes.browser.history

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.suihan74.satena.models.browser.BrowserDao
import com.suihan74.satena.models.browser.History
import com.suihan74.satena.models.browser.HistoryLog
import com.suihan74.satena.models.browser.HistoryPage
import com.suihan74.utilities.extensions.faviconUrl
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
    val histories = MutableLiveData<List<History>>(emptyList())

    /** ロード済みの全閲覧履歴データのキャッシュ */
    private var historiesCache = ArrayList<History>()
    private val historiesCacheLock = Mutex()

    /** 検索ワード */
    val keyword = MutableLiveData<String?>(null)

    // ------ //

    /** 履歴を追加する */
    suspend fun insertHistory(
        url: String,
        title: String,
        faviconUrl: String? = null
    ) = withContext(Dispatchers.Default) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val favicon = faviconUrl ?: Uri.parse(url).faviconUrl
        val decodedUrl = Uri.decode(url)

        val page = dao.getHistoryPage(decodedUrl) ?: HistoryPage(
            url = decodedUrl,
            title = title,
            faviconUrl = favicon,
            lastVisited = now
        )

        val visited = HistoryLog(visitedAt = now)

        val history = History(
            page = page,
            log = visited
        )
        dao.insertHistory(history)
        val inserted = dao.getHistory(now)

        if (inserted != null) {
            historiesCacheLock.withLock {
                historiesCache.removeAll {
                    it.log.visitedAt.toLocalDate() == today
                            && it.page.url == decodedUrl
                }
                historiesCache.add(inserted)
            }
        }

        updateHistoriesLiveData()
    }

    /** 履歴を削除する */
    suspend fun deleteHistory(history: History) = withContext(Dispatchers.Default) {
        dao.deleteHistoryLog(history.log)

        historiesCacheLock.withLock {
            historiesCache.removeAll { it.log.id == history.log.id }
        }
        updateHistoriesLiveData()
    }

    /** 履歴リストを更新 */
    suspend fun loadHistories() = withContext(Dispatchers.Default) {
        historiesCacheLock.withLock {
            historiesCache.clear()
            historiesCache.addAll(
                dao.getRecentHistories()
            )
        }

        updateHistoriesLiveData()
    }

    /** 履歴をすべて削除 */
    suspend fun clearHistories() = withContext(Dispatchers.Default) {
        dao.clearHistory()
        loadHistories()
    }

    /** 指定した日付の履歴をすべて削除 */
    suspend fun clearHistories(date: LocalDate) = withContext(Dispatchers.Default) {
        val start = date.atTime(0, 0)
        val end = date.plusDays(1L).atTime(0, 0)
        dao.deleteHistory(start, end)

        historiesCacheLock.withLock {
            historiesCache.removeAll { h ->
                h.log.visitedAt.toLocalDate() == date
            }
        }

        updateHistoriesLiveData()
    }

    /** 履歴リストの続きを取得 */
    suspend fun loadAdditional() = withContext(Dispatchers.Default) {
        historiesCacheLock.withLock {
            val additional = dao.getRecentHistories(offset = historiesCache.size)

            historiesCache.addAll(additional)
            historiesCache.sortBy { it.log.visitedAt }
        }

        updateHistoriesLiveData()
    }

    /** 表示用の履歴リストを更新する */
    suspend fun updateHistoriesLiveData() = withContext(Dispatchers.Default) {
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
                        it.page.title.toLowerCase(locale).contains(k)
                                || it.page.url.toLowerCase(locale).contains(k)
                                || it.log.visitedAt.format(dateTimeFormatter).contains(k)
                    }
                }
                histories.postValue(list)
            }
        }
    }
}
