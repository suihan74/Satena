package com.suihan74.satena.scenes.browser.history

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.browser.*
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.estimatedHierarchy
import com.suihan74.utilities.extensions.toSystemZonedDateTime
import com.suihan74.utilities.getSha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class HistoryRepository(
    private val prefs: SafeSharedPreferences<BrowserSettingsKey>,
    private val dao: BrowserDao
) {
    /** 閲覧履歴 */
    val histories = MutableLiveData<List<History>>()
    private val historiesCacheLock = Mutex()

    /** 検索ワード */
    val keyword = MutableLiveData<String?>(null)

    // ------ //

    /** 履歴を追加する */
    private suspend fun insertHistory(context: Context, url: String, title: String) = withContext(Dispatchers.Default) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val decodedUrl = Uri.decode(url)

        val faviconInfoId =
            Uri.parse(url).estimatedHierarchy?.let { domain -> dao.findFaviconInfo(domain)?.id } ?: 0L

        val page =
            dao.getHistoryPage(decodedUrl)?.let {
                it.copy(visitTimes = it.visitTimes + 1, faviconInfoId = faviconInfoId)
            } ?: HistoryPage(
                url = decodedUrl,
                title = title,
                lastVisited = now,
                faviconInfoId = faviconInfoId
            )

        val visited = HistoryLog(visitedAt = now)

        dao.insertHistory(page = page, log = visited)
        dao.getHistory(now)?.let { inserted ->
            clearOldHistories(context)
            historiesCacheLock.withLock {
                val items =
                    histories.value.orEmpty()
                        .filterNot {
                            it.log.visitedAt.toSystemZonedDateTime("UTC").toLocalDate().equals(today)
                                    && it.page.page.url == inserted.page.page.url
                        }.plus(inserted)
                histories.postValue(items)
            }
        }
    }

    suspend fun insertOrUpdateHistory(context: Context, url: String, title: String) = withContext(Dispatchers.Default) {
        val decodedUrl = Uri.decode(url)
        val existed = dao.getRecentHistories(limit = 1).firstOrNull()
        val existedPage = existed?.page
        if (existedPage == null || existedPage.page.url != decodedUrl) {
            insertHistory(context, url, title)
        }
        else {
            historiesCacheLock.withLock {
                val domain = Uri.parse(url).estimatedHierarchy!!
                val faviconInfo = existedPage.faviconInfo ?: dao.findFaviconInfo(domain)
                val updated = existedPage.page.copy(title = title, faviconInfoId = faviconInfo?.id ?: 0)
                dao.updateHistoryPage(updated)
                histories.postValue(
                    histories.value.orEmpty().map {
                        if (it.page.page.id == existedPage.page.id) it.copy(page = existedPage.copy(page = updated))
                        else it
                    }
                )
            }
        }
    }

    /** 履歴を削除する */
    suspend fun deleteHistory(history: History) = withContext(Dispatchers.Default) {
        dao.deleteHistoryLog(history.log)

        historiesCacheLock.withLock {
            histories.postValue(histories.value.orEmpty().filterNot { it.log.id == history.log.id })
        }
    }

    /** 履歴リストを更新 */
    suspend fun loadHistories() = withContext(Dispatchers.Default) {
        historiesCacheLock.withLock {
            runCatching {
                histories.postValue(
                    dao.findHistory(query = keyword.value.orEmpty())
                )
            }.onFailure {
                dao.restoreHistoryTable_v192()
                FirebaseCrashlytics.getInstance().recordException(RuntimeException("v190 history issue has fixed"))
                histories.postValue(
                    dao.findHistory(query = keyword.value.orEmpty())
                )
            }
        }
    }

    /** 履歴をすべて削除 */
    suspend fun clearHistories(context: Context) = withContext(Dispatchers.Default) {
        runCatching {
            dao.clearHistory()
            File(context.filesDir, "favicon_cache").deleteRecursively()
        }
        loadHistories()
    }

    /** 指定した日付の履歴をすべて削除 */
    suspend fun clearHistories(date: LocalDate) = withContext(Dispatchers.Default) {
        val start = date.atTime(0, 0)
        val end = date.plusDays(1L).atTime(0, 0)
        dao.deleteHistory(start, end)

        historiesCacheLock.withLock {
            histories.postValue(
                histories.value.orEmpty().filterNot { h ->
                    h.log.visitedAt.toSystemZonedDateTime("UTC").toLocalDate() == date
                }
            )
        }
    }

    /** 履歴リストの続きを取得 */
    suspend fun loadAdditional() = withContext(Dispatchers.Default) {
        historiesCacheLock.withLock {
            val additional = runCatching {
                dao.findHistory(query = keyword.value.orEmpty(), offset = histories.value?.size ?: 0)
            }.getOrElse {
                dao.restoreHistoryTable_v192()
                FirebaseCrashlytics.getInstance().recordException(RuntimeException("v190 history issue has fixed"))
                dao.findHistory(query = keyword.value.orEmpty(), offset = histories.value?.size ?: 0)
            }
            if (additional.isEmpty()) return@withContext

            histories.postValue(
                histories.value.orEmpty()
                    .plus(additional)
                    .sortedBy { it.log.visitedAt }
            )
        }
    }

    /** 寿命切れの履歴を削除する */
    private suspend fun clearOldHistories(context: Context) {
        val now = ZonedDateTime.now()
        val today = now.toLocalDate()
        val lifeSpanDays = prefs.getInt(BrowserSettingsKey.HISTORY_LIFESPAN)
        val lastRefreshed = prefs.getObject<ZonedDateTime>(BrowserSettingsKey.HISTORY_LAST_REFRESHED)
        if (lifeSpanDays == 0 || lastRefreshed != null && lastRefreshed.toLocalDate() >= today) {
            return
        }
        val threshold = now.toLocalDateTime().minusDays(lifeSpanDays.toLong())

        runCatching {
            dao.deleteHistory(LocalDateTime.MIN, threshold)
            dao.deleteHistoryPages(LocalDateTime.MIN, threshold)
        }

        historiesCacheLock.withLock {
            histories.postValue(
                histories.value.orEmpty().filter {
                    it.log.visitedAt >= threshold
                }
            )
        }

        // 参照されなくなったfaviconキャッシュを削除する
        clearOldFavicons(context)

        prefs.editSync {
            putObject(BrowserSettingsKey.HISTORY_LAST_REFRESHED, now)
        }
    }

    fun clearLastRefreshed() {
        prefs.edit {
            remove(BrowserSettingsKey.HISTORY_LAST_REFRESHED)
        }
    }

    // ------ //

    private val faviconMutex = Mutex()

    suspend fun findFaviconInfo(url: String) : FaviconInfo? = faviconMutex.withLock {
        runCatching {
            val site = Uri.parse(url).estimatedHierarchy ?: return null
            dao.findFaviconInfo(site)
        }.getOrNull()
    }

    /** 読み込んだfaviconをアプリディレクトリ内にキャッシュ */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun saveFaviconCache(context: Context, bitmap: Bitmap, url: String?) = withContext(Dispatchers.IO) {
        faviconMutex.withLock {
            runCatching {
                val site = Uri.parse(url).estimatedHierarchy!!
                val existed = SatenaApplication.instance.browserDao.findFaviconInfo(site)

                val filename =
                    ByteBuffer.allocate(bitmap.byteCount).let { buffer ->
                        bitmap.copyPixelsToBuffer(buffer)
                        getSha256(buffer.array())
                    }

                // ファイルがDB上で見つかるキャッシュは再書き込みしない
                if (existed?.filename == filename) {
                    return@runCatching
                }

                File(context.filesDir, "favicon_cache").let { dir ->
                    dir.mkdirs()
                    val outFile = File(dir, filename)
                    runCatching {
                        if (!outFile.exists()) {
                            val byteArray =
                                ByteArrayOutputStream().use { ostream ->
                                    val compressFormat =
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) Bitmap.CompressFormat.PNG
                                        else Bitmap.CompressFormat.WEBP_LOSSY
                                    bitmap.compress(compressFormat, 100, ostream)
                                    ostream.toByteArray()
                                }
                            outFile.outputStream().use { it.write(byteArray) }
                        }
                        insertFaviconInfo(filename, url, site, existed)
                    }.onFailure {
                        runCatching { outFile.delete() }
                    }
                }
            }.onFailure {
                // faviconロード完了時にブラウザが閉じられているとヌルポ発生する可能性あり
                Log.e("faviconCache", "failed to save a favicon cache for URL(${url ?: "null"})")
                //FirebaseCrashlytics.getInstance().recordException(it)
            }
        }
    }

    private suspend fun insertFaviconInfo(filename: String, url: String?, site: String, existed: FaviconInfo?) {
        url!!
        SatenaApplication.instance.browserDao.let { dao ->
            if (existed != null) {
                dao.updateFaviconInfo(
                    existed.copy(
                        site = site,
                        filename = filename,
                        lastUpdated = ZonedDateTime.now()
                    )
                )
            }
            else {
                dao.insertFaviconInfo(FaviconInfo(site, filename, ZonedDateTime.now()))
            }
            dao.getHistoryPage(Uri.decode(url))?.let { historyPage ->
                val faviconInfo = dao.findFaviconInfo(site)!!
                dao.updateHistoryPage(historyPage.copy(faviconInfoId = faviconInfo.id))
                updateHistoryFavicon(faviconInfo)
            }
        }
    }

    private suspend fun updateHistoryFavicon(faviconInfo: FaviconInfo) = withContext(Dispatchers.Default) {
        historiesCacheLock.withLock {
            histories.postValue(
                histories.value.orEmpty()
                    .map { h ->
                        if (h.page.page.faviconInfoId == faviconInfo.id || (h.page.page.faviconInfoId == 0L && Uri.parse(h.page.page.url).estimatedHierarchy == faviconInfo.site)) {
                            val updatedPage = h.page.page.copy(faviconInfoId = faviconInfo.id)
                            if (h.page.page.faviconInfoId == 0L) {
                                dao.updateHistoryPage(updatedPage)
                            }
                            h.copy(page = h.page.copy(page = updatedPage, faviconInfo = faviconInfo))
                        }
                        else h
                    }
            )
        }
    }

    /**
     * 使用されていないFaviconInfoを削除する
     */
    private suspend fun clearOldFavicons(context: Context) {
        faviconMutex.withLock {
            runCatching {
                val oldItems = dao.findOldFaviconInfo()
                dao.deleteFaviconInfo(oldItems)
                File(context.filesDir, "favicon_cache").let { dir ->
                    for (item in oldItems) {
                        File(dir, item.filename).delete()
                    }
                }
            }.onFailure {
                FirebaseCrashlytics.getInstance().recordException(it)
            }
        }
        clearUnManagedFavicons(context)
    }

    private var clearUnManagedFaviconsCalled = false

    /**
     * すべてのFaviconInfoから参照されていないのに存在するキャッシュファイルを削除する
     */
    private suspend fun clearUnManagedFavicons(context: Context) {
        faviconMutex.withLock {
            if (clearUnManagedFaviconsCalled) return
            runCatching {
                buildList {
                    File(context.filesDir, "favicon_cache").listFiles { dir, filename ->
                        add(filename)
                    }
                }.filterNot { filename ->
                    dao.existFaviconInfo(filename)
                }.forEach { filename ->
                    File("${context.filesDir.absolutePath}/favicon_cache/$filename").delete()
                }
            }.onFailure {
                FirebaseCrashlytics.getInstance().recordException(it)
            }
            clearUnManagedFaviconsCalled = true
        }
    }
}
