package com.suihan74.satena.models

import android.content.Context
import android.util.Log
import com.suihan74.satena.SatenaApplication
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Type

/**************************************
 * version 1
 **************************************/

/** お気に入りサイトのエントリを取得するための情報 */
@Deprecated("migrated to db")
data class FavoriteSite (
    /** サイトURL */
    val url: String,

    /** サイトタイトル */
    val title: String,

    /** ファビコン画像のURL */
    val faviconUrl: String,

    /** 有効状態(フィードを取得して画面に表示するか否か) */
    val isEnabled: Boolean
) {
    fun same(other: FavoriteSite?) : Boolean {
        if (other == null) return false
        return url == other.url &&
                title == other.title &&
                faviconUrl == other.faviconUrl &&
                isEnabled == other.isEnabled
    }
}

/**
 * お気に入りサイト登録情報
 */
@Deprecated("migrated to db")
@SharedPreferencesKey(fileName = "favorite_sites", version = 1, latest = true)
enum class FavoriteSitesKey (
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {
    /** エントリ履歴 */
    @Suppress("deprecation")
    SITES(typeInfo<List<FavoriteSite>>(), emptyList<FavoriteSite>())
}

// ------ //

@Suppress("deprecation")
object FavoriteSitesKeyMigration {
    fun check(context: Context) {
        while (true) {
            when (SafeSharedPreferences.version<FavoriteSitesKey>(context)) {
                0 -> migrateFromVersion0(context)
                else -> break
            }
        }
    }

    private fun migrateFromVersion0(context: Context) = runBlocking(Dispatchers.IO) {
        val repo = SatenaApplication.instance.favoriteSitesRepository
        val prefs = SafeSharedPreferences.create<FavoriteSitesKey>(context)
        runCatching {
            val sites = prefs.get<List<FavoriteSite>>(FavoriteSitesKey.SITES)
            for (site in sites) {
                repo.favoritePage(site.url, site.title, site.faviconUrl, site.isEnabled)
            }
            SafeSharedPreferences.delete<FavoriteSitesKey>(context)
        }.onFailure {
            Log.e("favoriteSites", it.stackTraceToString())
            throw it
        }
    }
}
