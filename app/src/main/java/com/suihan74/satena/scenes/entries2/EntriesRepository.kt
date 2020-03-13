package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.LiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.EntriesHistoryKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
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

    /** エントリ項目クリック時の挙動 */
    val entryClickedAction : TapEntryAction
        get() = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION))

    /** エントリ項目長押し時の挙動 */
    val entryLongClickedAction : TapEntryAction
        get() = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_LONG_TAP_ACTION))

    /** 初期化処理 */
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

    /** 最新のエントリーリストを読み込む */
    suspend fun loadEntries(category: Category, issue: Issue?, tabPosition: Int, offset: Int? = null) : List<Entry> =
        if (issue == null) loadEntries(category, tabPosition, offset)
        else loadEntries(issue, tabPosition, offset)

    /** 最新のエントリーリストを読み込む(Category指定) */
    private suspend fun loadEntries(category: Category, tabPosition: Int, offset: Int? = null) : List<Entry> =
        when (val apiCat = category.categoryInApi) {
            null -> loadSpecificEntries(category, tabPosition)
            else -> loadHatenaEntries(tabPosition, apiCat, offset)
        }

    /** はてなから提供されているカテゴリ以外のエントリ情報を取得する */
    private suspend fun loadSpecificEntries(category: Category, tabPosition: Int, offset: Int? = null) : List<Entry> =
        when (category) {
            Category.History -> loadHistory()

            Category.MyHotEntries -> client.getMyHotEntriesAsync().await()

            Category.MyBookmarks -> loadMyBookmarks(tabPosition, offset)

            else -> throw NotImplementedError("refreshing \"${category.name}\" is not implemented")
        }

    /** はてなの通常のエントリーリストを取得する */
    private suspend fun loadHatenaEntries(tabPosition: Int, category: com.suihan74.hatenaLib.Category, offset: Int?) : List<Entry> {
        val entriesType = EntriesType.fromInt(tabPosition)
        return client.getEntriesAsync(
            entriesType = entriesType,
            category = category,
            of = offset
        ).await()
    }

    /** エントリ閲覧履歴を取得する */
    private fun loadHistory() : List<Entry> =
        historyPrefs.get<List<Entry>>(EntriesHistoryKey.ENTRIES).reversed()

    /** マイブックマークを取得する */
    private suspend fun loadMyBookmarks(tabPosition: Int, offset: Int?) : List<Entry> =
        if (tabPosition == 0) client.getMyBookmarkedEntriesAsync(of = offset).await()
        else client.searchMyEntriesAsync("あとで読む", SearchType.Tag).await()

    /** 最新のエントリーリストを読み込む(Issue指定) */
    private suspend fun loadEntries(issue: Issue, tabPosition: Int, offset: Int? = null) : List<Entry> {
        val entriesType = EntriesType.fromInt(tabPosition)
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
