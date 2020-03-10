package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.LiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.EntriesHistoryKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EntriesRepository(
    private val client: HatenaClient,
    private val accountLoader: AccountLoader,
    private val prefs: SafeSharedPreferences<PreferenceKey>,
    private val historyPrefs: SafeSharedPreferences<EntriesHistoryKey>,
    private val ignoredEntryDao: IgnoredEntryDao
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

    /** スクロールにあわせてツールバーを隠す */
    val hideToolbarByScroll : Boolean
        get() = prefs.getBoolean(PreferenceKey.ENTRIES_HIDING_TOOLBAR_BY_SCROLLING)

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
    suspend fun loadEntries(category: Category, tabPosition: Int, offset: Int? = null) : List<Entry> {
        return when (val apiCat = category.categoryInApi) {
            null -> loadSpecificEntries(category, tabPosition)
            else -> {
                val entriesType = EntriesType.fromInt(tabPosition)
                client.getEntriesAsync(
                    entriesType = entriesType,
                    category = apiCat,
                    of = offset
                ).await()
            }
        }
    }

    /** はてなから提供されているカテゴリ以外のエントリ情報を取得する */
    private suspend fun loadSpecificEntries(category: Category, tabPosition: Int, offset: Int? = null) : List<Entry> {
        return when (category) {
            Category.History -> historyPrefs.get(EntriesHistoryKey.ENTRIES)

            Category.MyHotEntries -> client.getMyHotEntriesAsync().await()

            Category.MyBookmarks ->
                if (tabPosition == 0) client.getMyBookmarkedEntriesAsync(of = offset).await()
                else client.searchMyEntriesAsync("あとで読む", SearchType.Tag).await()

            else -> throw NotImplementedError("refreshing \"${category.name}\" is not implemented")
        }
    }

    /** 最新のエントリーリストを読み込む(Issue指定) */
    suspend fun loadEntries(issue: Issue, entriesType: EntriesType, offset: Int? = null) : List<Entry> {
        return client.getEntriesAsync(
            entriesType = entriesType,
            issue = issue,
            of = offset
        ).await()
    }

    /** エントリをフィルタリングする */
    suspend fun filterEntries(entries: List<Entry>) : List<Entry> = withContext(Dispatchers.IO) {
        val ignoredEntries = ignoredEntryDao.getAllEntries()
        return@withContext entries.filterNot { entry ->
            ignoredEntries.any { it.isMatched(entry) }
        }
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
