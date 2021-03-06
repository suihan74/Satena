package com.suihan74.satena.scenes.preferences.favoriteSites

import androidx.annotation.MainThread
import com.suihan74.hatenaLib.ConnectionFailureException
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.NotFoundException
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.models.FavoriteSitesKey
import com.suihan74.utilities.PreferenceLiveData
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.exceptions.AlreadyExistedException
import com.suihan74.utilities.exceptions.TaskFailureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoriteSitesRepository(
    prefs : SafeSharedPreferences<FavoriteSitesKey>,
    private val client : HatenaClient? = null,
) {
    val favoriteSites =
        PreferenceLiveData(prefs, FavoriteSitesKey.SITES) { p, key ->
            p.get<List<FavoriteSite>>(key)
        }

    // ------ //

    /** 指定URLが既に登録されているか確認する */
    fun contains(url: String) : Boolean {
        val list = favoriteSites.value.orEmpty()
        return list.any { it.url == url }
    }

    // ------ //

    /**
     * ページをお気に入りに登録する
     *
     * @throws AlreadyExistedException
     */
    @MainThread
    fun favoritePage(url: String, title: String, faviconUrl: String) {
        favoritePage(FavoriteSite(url, title, faviconUrl, false))
    }

    /**
     * ページをお気に入りに登録する
     *
     * @throws AlreadyExistedException
     */
    @MainThread
    fun favoritePage(site: FavoriteSite) {
        if (contains(site.url)) {
            throw AlreadyExistedException("the site has already existed as the favorite site: ${site.url}")
        }
        val list = favoriteSites.value.orEmpty()
        favoriteSites.value = list.plus(site)
    }

    /**
     * サイトをお気に入りから除外する
     *
     * @throws NotFoundException
     */
    @MainThread
    fun unfavoritePage(site: FavoriteSite) {
        val list = favoriteSites.value ?: return
        val target = list.firstOrNull { it.same(site) } ?: throw NotFoundException("the favorite site is not found: ${site.url}")
        favoriteSites.value = list.minus(target)
    }

    // ------ //

    /**
     * エントリのサイトをお気に入りに追加
     *
     * @throws AlreadyExistedException
     * @throws TaskFailureException
     */
    suspend fun favoriteEntrySite(entry: Entry) = withContext(Dispatchers.Default) {
        val sites = favoriteSites.value.orEmpty()
        val url = entry.rootUrl

        if (sites.any { it.url == url }) {
            throw AlreadyExistedException()
        }

        val titleResult = runCatching { client?.getSiteTitle(url) ?: entry.url }
        val title = when {
            titleResult.isSuccess -> titleResult.getOrNull() ?: entry.url

            titleResult.exceptionOrNull() is ConnectionFailureException -> url

            else -> throw TaskFailureException(cause = titleResult.exceptionOrNull())
        }

        val newList = sites.plus(FavoriteSite(
            url = url,
            title = title,
            faviconUrl = entry.faviconUrl,
            isEnabled = true
        ))

        favoriteSites.postValue(newList)
    }

    /**
     * エントリのサイトをお気に入りから削除する
     *
     * @throws NotFoundException
     */
    @MainThread
    fun unfavoriteEntrySite(entry: Entry) {
        val sites = favoriteSites.value.orEmpty()

        if (!sites.any { it.url == entry.rootUrl }) {
            throw NotFoundException("the favorite site is not found: ${entry.rootUrl}")
        }

        val newList = sites.filter { it.url != entry.rootUrl }
        favoriteSites.value = newList
    }
}
