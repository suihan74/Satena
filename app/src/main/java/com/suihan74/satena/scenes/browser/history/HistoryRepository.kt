package com.suihan74.satena.scenes.browser.history

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.suihan74.satena.models.browser.BrowserDao
import com.suihan74.satena.models.browser.History
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime

class HistoryRepository(
    private val dao: BrowserDao
) {

    /** 閲覧履歴 */
    val histories by lazy {
        MutableLiveData<List<History>>(emptyList())
    }

    /** ロード済みの全閲覧履歴データのキャッシュ */
    private var historiesCache = ArrayList<History>()

    /** 検索ワード */
    val keyword by lazy {
        MutableLiveData<String?>(null)
    }

    // ------ //

    /** 初期化処理 */
    suspend fun initialize() {
        reloadHistories()
    }

    /**
     * URLからファビコンURLを取得する
     */
    fun getFaviconUrl(uri: Uri) : String = "https://www.google.com/s2/favicons?domain=${uri.host}"

    /** (代替の)faviconのURLを取得する */
    fun getFaviconUrl(url: String) : String = getFaviconUrl(Uri.parse(url))

    /** 履歴を追加する */
    suspend fun insertHistory(
        url: String,
        title: String,
        faviconUrl: String? = null
    ) = withContext(Dispatchers.IO) {
        val history = History(
            url = Uri.decode(url),
            title = title,
            faviconUrl = faviconUrl ?: getFaviconUrl(url),
            lastVisited = LocalDateTime.now()
        )
        dao.insertHistory(history)

        historiesCache.removeAll { it.url == history.url }
        historiesCache.add(history)

        updateHistoriesLiveData()
    }

    /** 履歴リストを更新 */
    suspend fun reloadHistories() = withContext(Dispatchers.IO) {
        historiesCache.clear()
        historiesCache.addAll(
            dao.getRecentHistories()
        )

        updateHistoriesLiveData()
    }

    /** 履歴をすべて削除 */
    suspend fun clearHistories() = withContext(Dispatchers.IO) {
        dao.clearHistory()
        reloadHistories()
    }

    /** 履歴リストの続きを取得 */
    suspend fun loadAdditional() = withContext(Dispatchers.IO) {
        val additional = dao.getRecentHistories(offset = historiesCache.size)

        historiesCache.addAll(additional)
        historiesCache.sortBy { it.lastVisited }

        updateHistoriesLiveData()
    }

    /** 表示用の履歴リストを更新する */
    suspend fun updateHistoriesLiveData() = withContext(Dispatchers.IO) {
        val keyword = keyword.value
        if (keyword.isNullOrBlank()) {
            histories.postValue(historiesCache)
        }
        else {
            val list = historiesCache.filter {
                it.title.contains(keyword) || it.url.contains(keyword)
            }
            histories.postValue(list)
        }
    }
}
