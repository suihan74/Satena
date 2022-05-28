package com.suihan74.satena.scenes.preferences.favoriteSites

import android.net.Uri
import android.webkit.URLUtil
import com.suihan74.hatenaLib.ConnectionFailureException
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.NotFoundException
import com.suihan74.satena.models.favoriteSite.FavoriteSite
import com.suihan74.satena.models.favoriteSite.FavoriteSiteAndFavicon
import com.suihan74.satena.models.favoriteSite.FavoriteSiteDao
import com.suihan74.utilities.exceptions.AlreadyExistedException
import com.suihan74.utilities.exceptions.EmptyException
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.suihan74.utilities.exceptions.TaskFailureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoriteSitesRepository(
    private val dao : FavoriteSiteDao
) {
    val favoriteSitesFlow = dao.allFavoriteSitesFlow()

    // ------ //

    /** 指定URLが既に登録されているか確認する */
    suspend fun contains(url: String) : Boolean = dao.exists(url)

    suspend fun get(url: String) : FavoriteSiteAndFavicon? = dao.findFavoriteSite(url)

    suspend fun allSites() = dao.allFavoriteSites()

    // ------ //

    /**
     * ページをお気に入りに登録する
     *
     * @throws AlreadyExistedException
     */
    suspend fun favoritePage(
        url: String,
        title: String,
        faviconUrl: String,
        isEnabled: Boolean = false,
        modify: Boolean = false,
        id: Long = 0
    ) = favoritePage(FavoriteSite(url, title, isEnabled, 0, faviconUrl, id), modify)

    /**
     * ページをお気に入りに登録する
     *
     * @param site 対象サイト
     * @param modify 既に登録されているサイトを更新する
     *
     * @throws AlreadyExistedException 既にお気に入りのURL
     * @throws InvalidUrlException URLが不正
     * @throws EmptyException タイトルが空白
     */
    suspend fun favoritePage(site: FavoriteSite, modify: Boolean = false) {
        if (!URLUtil.isValidUrl(site.url)
            || !URLUtil.isHttpsUrl(site.url)
            || !URLUtil.isHttpsUrl(site.url)
            || Uri.parse(site.url).host.isNullOrBlank()
        ) {
            throw InvalidUrlException(site.url)
        }

        if (site.title.isBlank()) {
            throw EmptyException()
        }

        val domain =
            runCatching { Uri.parse(site.url).host!! }.getOrElse { throw InvalidUrlException(url = site.url) }

        val existed = dao.findFavoriteSite(site.url)

        if (modify) {
            if (existed != null && existed.site.id != site.id) {
                throw AlreadyExistedException("the site has already existed as the favorite site: ${site.url}")
            }
            dao.update(
                dao.findFaviconInfo(domain)?.let { site.copy(faviconInfoId = it.id) } ?: site
            )
        }
        else {
            if (existed != null) {
                throw AlreadyExistedException("the site has already existed as the favorite site: ${site.url}")
            }
            dao.insert(
                dao.findFaviconInfo(domain)?.let { site.copy(faviconInfoId = it.id) } ?: site
            )
        }
    }

    /**
     * 既に登録されているお気に入りサイトの内容を更新。主に`FavoriteSite#isEnabled`の更新に使用
     *
     * @throws TaskFailureException
     */
    suspend fun update(sites: List<FavoriteSite>) {
        runCatching {
            dao.update(sites)
        }.onFailure {
            throw TaskFailureException(cause = it)
        }
    }

    /**
     * faviconInfoと紐づいていないお気に入りサイトにfaviconInfoを紐づける
     */
    suspend fun updateFavicon(domain: String) {
        runCatching {
            val faviconInfo = dao.findFaviconInfo(domain) ?: return
            val items = dao.findItemsFaviconInfoNotSet()
                .filter { Uri.parse(it.url).host == domain }
                .map { it.copy(faviconInfoId = faviconInfo.id) }
            dao.update(items)
        }
    }

    /**
     * サイトをお気に入りから除外する
     *
     * @throws NotFoundException
     */
    suspend fun unfavoritePage(site: FavoriteSiteAndFavicon) = unfavoritePage(site.site)

    /**
     * サイトをお気に入りから除外する
     *
     * @throws NotFoundException
     * @throws TaskFailureException
     */
    suspend fun unfavoritePage(site: FavoriteSite) {
        runCatching {
            val existed = dao.findFavoriteSite(site.url)
            if (existed?.site?.same(site) != true) {
                throw NotFoundException("the favorite site is not found: ${site.url}")
            }
            dao.delete(existed.site)
        }.onFailure { e ->
            throw if (e is NotFoundException) e else TaskFailureException(cause = e)
        }
    }

    // ------ //

    /**
     * エントリのサイトをお気に入りに追加
     *
     * @throws AlreadyExistedException
     * @throws TaskFailureException
     */
    suspend fun favoriteEntrySite(entry: Entry) = withContext(Dispatchers.Default) {
        val url = entry.rootUrl
        if (dao.exists(url)) throw AlreadyExistedException()
        val title =
            runCatching { HatenaClient.getSiteTitle(url) }.let { result ->
                when {
                    result.isSuccess ->
                        result.getOrNull().let { title ->
                            if (title.isNullOrBlank()) entry.rootUrl
                            else title
                        }

                    result.exceptionOrNull() is ConnectionFailureException -> url

                    else -> throw TaskFailureException(cause = result.exceptionOrNull())
                }
            }
        favoritePage(url, title, entry.faviconUrl, true)
    }

    /**
     * エントリのサイトをお気に入りから削除する
     *
     */
    suspend fun unfavoriteEntrySite(entry: Entry) {
        dao.delete(entry.rootUrl)
    }
}
