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

        val prevList = histories.value ?: emptyList()
        val currentList = prevList
            .filterNot { it.url == history.url }
            .plus(history)

        histories.postValue(currentList)
    }

    /** 履歴リストを更新 */
    suspend fun reloadHistories() = withContext(Dispatchers.IO) {
        histories.postValue(
            dao.getRecentHistories()
        )
    }

    /** 履歴をすべて削除 */
    suspend fun clearHistories() = withContext(Dispatchers.IO) {
        dao.clearHistory()
        reloadHistories()
    }

    /** 履歴リストの続きを取得 */
    suspend fun loadAdditional() = withContext(Dispatchers.IO) {
        val prevList = histories.value ?: emptyList()
        val additional = dao.getRecentHistories(offset = prevList.size)
        val currentList = additional.plus(prevList)
            .sortedBy { it.lastVisited }

        histories.postValue(currentList)
    }
}
