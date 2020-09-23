package com.suihan74.satena.scenes.browser.favorites

import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.models.FavoriteSitesKey
import com.suihan74.utilities.PreferenceLiveData
import com.suihan74.utilities.SafeSharedPreferences

class FavoriteSitesRepository(
    private val prefs: SafeSharedPreferences<FavoriteSitesKey>
) {

    val sites by lazy {
        PreferenceLiveData(prefs, FavoriteSitesKey.SITES) { p, key ->
            p.get<List<FavoriteSite>>(key)
        }
    }

    // ------ //

    /** サイトをお気に入りに登録する */
    fun favorite(url: String, title: String, faviconUrl: String) {
        val list = sites.value ?: emptyList()
        if (list.none { it.url == url }) {
            val site = FavoriteSite(url, title, faviconUrl, isEnabled = false)
            sites.value = list.plus(site)
        }
    }

    /** サイトをお気に入りから除外する */
    fun unfavorite(site: FavoriteSite) {
        val list = sites.value ?: return
        sites.value = list.minus(site)
    }
}
