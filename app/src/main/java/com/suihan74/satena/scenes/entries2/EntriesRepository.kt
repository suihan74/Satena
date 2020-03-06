package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.LiveData
import com.suihan74.hatenaLib.EntriesType
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Issue
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.EntriesHistoryKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences

class EntriesRepository(
    private val client: HatenaClient,
    private val accountLoader: AccountLoader,
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

    suspend fun initialize(onError: ((Throwable)->Unit)? = null) {
        signIn(false, onError)
    }

    /** サインインする */
    suspend fun signIn(forceUpdate: Boolean = false, onError: ((Throwable)->Unit)? = null) {
        try {
            accountLoader.signInAccounts(forceUpdate)
            signedInLiveData.post(client.signedIn())
            categoriesLiveData.post(client.signedIn())
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }
    }

    /** 最新のエントリーリストを読み込む(Category指定) */
    suspend fun refreshEntries(category: Category, entriesType: EntriesType? = null) : List<Entry> {
        return when (val apiCat = category.categoryInApi) {
            null -> refreshSpecificEntries(category)
            else -> {
                client.getEntriesAsync(entriesType!!, apiCat).await()
            }
        }
    }

    /** はてなから提供されているカテゴリ以外のエントリ情報を取得する */
    private suspend fun refreshSpecificEntries(category: Category) : List<Entry> {
        return when (category) {
            Category.History -> historyPrefs.get(EntriesHistoryKey.ENTRIES)

            Category.MyHotEntries -> client.getMyHotEntriesAsync().await()

            else -> throw NotImplementedError("refreshing \"${category.name}\" is not implemented")
        }
    }

    /** 最新のエントリーリストを読み込む(Issue指定) */
    suspend fun refreshEntries(issue: Issue, entriesType: EntriesType) : List<Entry> {
        return client.getEntriesAsync(entriesType, issue).await()
    }

    /** サインイン状態の変更を通知する */
    inner class SignedInLiveData : LiveData<Boolean>(signedIn) {
        internal fun post(b: Boolean?) {
            postValue(b)
        }
    }

    /** カテゴリリストの変更を通知する */
    inner class CategoriesLiveData : LiveData<Array<Category>>(categories) {
        internal fun post(signedIn: Boolean?) {
            postValue(
                if (signedIn == true) Category.valuesWithSignedIn()
                else Category.valuesWithoutSignedIn()
            )
        }
    }
}
