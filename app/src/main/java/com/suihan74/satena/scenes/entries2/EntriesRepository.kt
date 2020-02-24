package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.LiveData
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.EntriesHistoryKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

class EntriesRepository(
    private val client: HatenaClient,
    private val prefs: SafeSharedPreferences<PreferenceKey>,
    private val historyPrefs: SafeSharedPreferences<EntriesHistoryKey>
) {
    /** サインイン状態 */
    val signedIn : Boolean
        get() = client.signedIn()

    val signedInLiveData = SignedInLiveData()

    /** ホームカテゴリ */
    val homeCategory : Category
        get() = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))

    /** 表示できるカテゴリのリスト */
    val categories : Array<Category>
        get() =
            if (signedIn) Category.valuesWithSignedIn()
            else Category.valuesWithoutSignedIn()

    val categoriesLiveData = CategoriesLiveData()

    /** ドロワーにタップ防止背景を使用する */
    val isFABMenuBackgroundActive : Boolean
        get() = prefs.getBoolean(PreferenceKey.ENTRIES_MENU_TAP_GUARD)

    /** サインイン状態の変更を通知する */
    inner class SignedInLiveData : LiveData<Boolean>(signedIn) {
    }

    /** カテゴリリストの変更を通知する */
    inner class CategoriesLiveData : LiveData<Array<Category>>(categories) {
    }
}
