package com.suihan74.satena.models

import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import java.lang.reflect.Type

/**************************************
 * version 1
 **************************************/

/** お気に入りサイトのエントリを取得するための情報 */
data class FavoriteSite (
    /** サイトURL */
    val url: String,

    /** サイトタイトル */
    val title: String,

    /** ファビコン画像のURL */
    val faviconUrl: String,

    /** 有効状態(フィードを取得して画面に表示するか否か) */
    val isEnabled: Boolean
)

/**
 * お気に入りサイト登録情報
 */
@SharedPreferencesKey(fileName = "favorite_sites", version = 0, latest = true)
enum class FavoriteSitesKey (
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {
    /** エントリ履歴 */
    SITES(typeInfo<List<FavoriteSite>>(), emptyList<FavoriteSite>())
}

