package com.suihan74.satena.scenes.browser.history

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.browser.BrowserDao
import com.suihan74.satena.models.browser.History
import com.suihan74.satena.models.browser.HistoryLog
import com.suihan74.satena.models.browser.HistoryPage
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.faviconUrl
import com.suihan74.utilities.extensions.toSystemZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZonedDateTime
import java.util.*
import kotlin.collections.ArrayList

class HistoryRepository(
    private val prefs: SafeSharedPreferences<BrowserSettingsKey>,
    private val dao: BrowserDao
) {

    /** 閲覧履歴 */
    val histories = MutableLiveData<List<History>>()

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
                    it.log.visitedAt.toSystemZonedDateTime("UTC").toLocalDate().equals(today)
                            && it.page.url == inserted.page.url
                }
                historiesCache.add(inserted)
                histories.postValue(historiesCache)
            }
        }
    }

    /** 履歴を削除する */
    suspend fun deleteHistory(history: History) = withContext(Dispatchers.Default) {
        dao.deleteHistoryLog(history.log)

        historiesCacheLock.withLock {
            historiesCache.removeAll { it.log.id == history.log.id }
            histories.postValue(historiesCache)
        }
    }

    /** 履歴リストを更新 */
    suspend fun loadHistories() = withContext(Dispatchers.Default) {
        clearOldHistories()
        historiesCacheLock.withLock {
            historiesCache.clear()
            historiesCache.addAll(
                dao.findHistory(query = keyword.value.orEmpty())
            )
            histories.postValue(historiesCache)
        }
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
                h.log.visitedAt.toSystemZonedDateTime("UTC").toLocalDate() == date
            }
            histories.postValue(historiesCache)
        }
    }

    /** 履歴リストの続きを取得 */
    suspend fun loadAdditional() = withContext(Dispatchers.Default) {
        historiesCacheLock.withLock {
            val additional = dao.findHistory(query = keyword.value.orEmpty(), offset = historiesCache.size)
            if (additional.isEmpty()) return@withContext

            historiesCache.addAll(additional)
            historiesCache.sortBy { it.log.visitedAt }
            histories.postValue(historiesCache)
        }
    }

    /** 寿命切れの履歴を削除する */
    private suspend fun clearOldHistories() {
        val now = ZonedDateTime.now()
        val today = now.toLocalDate()
        val lifeSpanDays = prefs.getInt(BrowserSettingsKey.HISTORY_LIFESPAN)
        val lastRefreshed = prefs.getObject<ZonedDateTime>(BrowserSettingsKey.HISTORY_LAST_REFRESHED)
        if (lastRefreshed != null && lastRefreshed.toLocalDate() >= today || lifeSpanDays == 0) {
            return
        }

        runCatching {
            val threshold = now.toLocalDateTime().minusDays(lifeSpanDays.toLong())
            dao.deleteHistory(LocalDateTime.MIN, threshold)
            dao.deleteHistoryPages(LocalDateTime.MIN, threshold)
        }

        prefs.editSync {
            putObject(BrowserSettingsKey.HISTORY_LAST_REFRESHED, now)
        }
    }
}
