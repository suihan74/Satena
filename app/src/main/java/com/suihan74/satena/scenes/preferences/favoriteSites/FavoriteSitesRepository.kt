package com.suihan74.satena.scenes.preferences.favoriteSites

import androidx.annotation.MainThread
import com.suihan74.hatenaLib.ConnectionFailureException
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.models.FavoriteSitesKey
import com.suihan74.utilities.PreferenceLiveData
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.exceptions.AlreadyExistedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 内部ブラウザ用のお気に入りサイトの処理 */
interface FavoriteSitesRepositoryForBrowser {
    val favoriteSites : PreferenceLiveData<SafeSharedPreferences<FavoriteSitesKey>, FavoriteSitesKey, List<FavoriteSite>>

    /** ページをお気に入りに登録する */
    @MainThread
    fun favoritePage(url: String, title: String, faviconUrl: String)

    /** サイトをお気に入りから除外する */
    fun unfavoriteSite(site: FavoriteSite)
}

// ------ //

/** エントリ画面用のお気に入りサイトの処理 */
interface FavoriteSitesRepositoryForEntries {
    val favoriteSites : PreferenceLiveData<SafeSharedPreferences<FavoriteSitesKey>, FavoriteSitesKey, List<FavoriteSite>>

    /** エントリのサイトをお気に入りに追加 */
    @Throws(Throwable::class, AlreadyExistedException::class)
    suspend fun favoriteEntrySite(entry: Entry)

    /** エントリのサイトをお気に入りから削除する */
    @MainThread
    fun unfavoriteEntrySite(entry: Entry)
}

// ------ //

class FavoriteSitesRepository(
    private val prefs : SafeSharedPreferences<FavoriteSitesKey>,
    private val client : HatenaClient? = null,
) :
    FavoriteSitesRepositoryForBrowser,
    FavoriteSitesRepositoryForEntries
{

    override val favoriteSites =
        PreferenceLiveData(prefs, FavoriteSitesKey.SITES) { p, key ->
            p.get<List<FavoriteSite>>(key)
        }

    // ------ //

    /** ページをお気に入りに登録する */
    @MainThread
    override fun favoritePage(url: String, title: String, faviconUrl: String) {
        val list = favoriteSites.value ?: emptyList()
        if (list.none { it.url == url }) {
            val site = FavoriteSite(url, title, faviconUrl, isEnabled = false)
            favoriteSites.value = list.plus(site)
        }
    }

    /** サイトをお気に入りから除外する */
    override fun unfavoriteSite(site: FavoriteSite) {
        val list = favoriteSites.value ?: return
        favoriteSites.value = list.minus(site)
    }

    // ------ //

    /** エントリのサイトをお気に入りに追加 */
    @Throws(Throwable::class, AlreadyExistedException::class)
    override suspend fun favoriteEntrySite(
        entry: Entry
    ) = withContext(Dispatchers.Default) {
        val sites = favoriteSites.value ?: emptyList()
        val url = entry.rootUrl

        if (sites.any { it.url == url }) {
            throw AlreadyExistedException()
        }

        val titleResult = runCatching { client?.getSiteTitle(url) ?: entry.url }
        val title = when {
            titleResult.isSuccess -> titleResult.getOrNull() ?: entry.url
            titleResult.exceptionOrNull() is ConnectionFailureException -> url
            else -> throw titleResult.exceptionOrNull()!!
        }

        val newList = sites.plus(FavoriteSite(
            url = url,
            title = title,
            faviconUrl = entry.faviconUrl,
            isEnabled = true
        ))

        favoriteSites.postValue(newList)
    }

    /** エントリのサイトをお気に入りから削除する */
    @MainThread
    override fun unfavoriteEntrySite(entry: Entry) {
        val sites = favoriteSites.value ?: emptyList()

        if (!sites.any { it.url == entry.rootUrl }) {
            return
        }

        val newList = sites.filter { it.url != entry.rootUrl }
        favoriteSites.value = newList
    }
}
