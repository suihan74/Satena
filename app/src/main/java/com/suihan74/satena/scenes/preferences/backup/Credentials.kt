package com.suihan74.satena.scenes.preferences.backup

import android.content.Context
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

class Credentials private constructor(
    val deviceId : String?,
    val hatenaRk : String?,
    val mstdnToken : String?
) {
    companion object {
        /**
         * 保存対象に含めないユーザー情報などを抽出する
         */
        suspend fun extract(context: Context) : Credentials {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val credentials = Credentials(
                deviceId = prefs.getString(PreferenceKey.ID),
                hatenaRk = prefs.getString(PreferenceKey.HATENA_RK),
                mstdnToken = prefs.getString(PreferenceKey.MASTODON_ACCESS_TOKEN)
            )
            prefs.editSync {
                remove(PreferenceKey.ID)
                remove(PreferenceKey.HATENA_RK)
                remove(PreferenceKey.MASTODON_ACCESS_TOKEN)
            }
            return credentials
        }
    }

    /**
     * 抽出したデータを`SafeSharedPreferences`に再登録する
     */
    suspend fun restore(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        prefs.editSync {
            putString(PreferenceKey.ID, deviceId)
            putString(PreferenceKey.HATENA_RK, hatenaRk)
            putString(PreferenceKey.MASTODON_ACCESS_TOKEN, mstdnToken)
        }
    }
}
