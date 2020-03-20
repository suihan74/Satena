package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.*
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.checkFromSpam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime

/** エントリリストの取得時にカテゴリによっては必要な必要な追加パラメータ */
class LoadEntryParameter {
    companion object {
        /** 検索クエリ(String) : Category.Search */
        const val SEARCH_QUERY = "LoadEntryParameter.SEARCH_QUERY"

        /** サイトURL(String) : Category.Site */
        const val SITE_URL = "LoadEntryParameter.SITE_URL"

        /** ページ(Int) : Category.Site */
        const val PAGE = "LoadEntryParameter.PAGE"

        /** タグ : Category.MyBookmarks */
        const val TAG = "LoadEntryParameter.TAG"

        /** ユーザー名 : Category.User */
        const val USER = "LoadEntryParameter.USER"
    }

    /** データ */
    private val map = HashMap<String, Any?>()

    /** 値を追加 */
    fun put(key: String, value: Any?) {
        map[key] = value
    }

    /** 値をT型として取得(失敗時null) */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String) : T? = map[key] as? T

    /** 値をT型として取得(失敗時default) */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, defaultValue: T) : T = (map[key] as? T) ?: defaultValue
}

class EntriesRepository(
    private val client: HatenaClient,
    private val accountLoader: AccountLoader,
    private val prefs: SafeSharedPreferences<PreferenceKey>,
    private val historyPrefs: SafeSharedPreferences<EntriesHistoryKey>,
    private val noticesPrefs: SafeSharedPreferences<NoticesKey>,
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
    suspend fun loadEntries(
        category: Category,
        issue: Issue?,
        tabPosition: Int,
        offset: Int? = null,
        params: LoadEntryParameter? = null
    ) : List<Entry> =
        if (issue == null) loadEntries(category, tabPosition, offset, params)
        else loadEntries(issue, tabPosition, offset)

    /** 最新のエントリーリストを読み込む(Category指定) */
    private suspend fun loadEntries(category: Category, tabPosition: Int, offset: Int?, params: LoadEntryParameter?) : List<Entry> =
        when (val apiCat = category.categoryInApi) {
            null -> loadSpecificEntries(category, tabPosition, offset, params)
            else -> loadHatenaEntries(tabPosition, apiCat, offset)
        }

    /** はてなから提供されているカテゴリ以外のエントリ情報を取得する */
    private suspend fun loadSpecificEntries(category: Category, tabPosition: Int, offset: Int?, params: LoadEntryParameter?) : List<Entry> =
        when (category) {
            Category.Site -> {
                val url = params?.get<String>(LoadEntryParameter.SITE_URL) ?: ""
                val page = params?.get<Int>(LoadEntryParameter.PAGE) ?: 0
                loadSiteEntries(url, tabPosition, page)
            }

            Category.History -> loadHistory()

            Category.MyHotEntries -> client.getMyHotEntriesAsync().await()

            Category.MyBookmarks -> loadMyBookmarks(tabPosition, offset, params)

            Category.User -> loadUserEntries(offset, params!!)

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

    /** ユーザーのタグ一覧を取得する */
    suspend fun loadUserTags(user: String) : List<Tag> = client.getUserTagsAsync(user).await()

    /** エントリ閲覧履歴を取得する */
    private fun loadHistory() : List<Entry> =
        historyPrefs.get<List<Entry>>(EntriesHistoryKey.ENTRIES).reversed()

    /** マイブックマークを取得する */
    private suspend fun loadMyBookmarks(tabPosition: Int, offset: Int?, params: LoadEntryParameter?) : List<Entry> {
        val tag = params?.get<String>(LoadEntryParameter.TAG)

        return if (tag == null) client.getMyBookmarkedEntriesAsync(of = offset).await()
               else client.searchMyEntriesAsync(tag, SearchType.Tag).await()
    }

    /** ユーザーがブクマしたエントリ一覧を取得する */
    private suspend fun loadUserEntries(offset: Int?, params: LoadEntryParameter) : List<Entry> {
        val user = params.get<String>(LoadEntryParameter.USER)!!
        val tag = params.get<String>(LoadEntryParameter.TAG)

        return client.getUserEntriesAsync(user = user, tag = tag, of = offset).await()
    }

    /** 通知リストを取得する */
    suspend fun loadNotices() : List<Notice> {
        val fetchedNotices = client.getNoticesAsync().await().notices

        // 通知の既読状態を更新
        val lastSeenUpdatable = prefs.getBoolean(PreferenceKey.NOTICES_LAST_SEEN_UPDATABLE)
        if (lastSeenUpdatable) {
            try {
                client.updateNoticesLastSeenAsync().await()
            }
            catch (e: Exception) {
            }
        }
        prefs.edit {
            putObject(PreferenceKey.NOTICES_LAST_SEEN, LocalDateTime.now())
        }

        val savedNotices = noticesPrefs.get<List<Notice>>(NoticesKey.NOTICES)
        val noticesSize = noticesPrefs.getInt(NoticesKey.NOTICES_SIZE)
        val removedNotices = noticesPrefs.get<List<NoticeTimestamp>>(NoticesKey.REMOVED_NOTICE_TIMESTAMPS)
        val ignoreNoticesFromSpam = prefs.getBoolean(PreferenceKey.IGNORE_NOTICES_FROM_SPAM)

        // 取得したものと重複しないように保存済みのものを取得する
        val oldNotices = savedNotices.filterNot { existed ->
            fetchedNotices.any { newer ->
                newer.created == existed.created
            }
        }

        val allNotices =
            if (ignoreNoticesFromSpam) oldNotices.plus(fetchedNotices).filterNot { it.checkFromSpam() }
            else oldNotices.plus(fetchedNotices)

        // 過去に削除指定されたものが含まれていたら除ける
        var oldestMatched = LocalDateTime.MAX
        val notices = allNotices
            .filterNot { n ->
                removedNotices.any { it.created == n.created && it.modified == n.modified }.also { result ->
                    if (result) {
                        oldestMatched = minOf(n.modified, oldestMatched)
                    }
                }
            }
            .sortedByDescending { it.modified }
            .take(noticesSize)

        noticesPrefs.edit {
            put(NoticesKey.NOTICES, notices)

            // 古い削除指定を消去する
            if (oldestMatched < LocalDateTime.MAX) {
                put(NoticesKey.REMOVED_NOTICE_TIMESTAMPS, removedNotices.filter {
                    it.modified >= oldestMatched
                })
            }
        }

        return notices
    }

    /** 最新のエントリーリストを読み込む(Issue指定) */
    private suspend fun loadEntries(issue: Issue, tabPosition: Int, offset: Int? = null) : List<Entry> {
        val entriesType = EntriesType.fromInt(tabPosition)
        return client.getEntriesAsync(
            entriesType = entriesType,
            issue = issue,
            of = offset
        ).await()
    }

    /** 指定したサイトのエントリーリストを読み込む */
    private suspend fun loadSiteEntries(url: String, tabPosition: Int, page: Int? = null) : List<Entry> {
        val entriesType = EntriesType.fromInt(tabPosition)
        return client.getEntriesAsync(
            url = url,
            entriesType = entriesType,
            allMode = true,
            page = page ?: 0
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

    /** Issueリストの変更を通知する */
    @OptIn(ExperimentalCoroutinesApi::class)
    inner class IssuesLiveData(
        private val categoryLiveData: MutableLiveData<Category>
    ) : LiveData<List<Issue>>() {
        override fun onActive() {
            categoryLiveData.observeForever { category ->
                if (!category.hasIssues) return@observeForever
                val apiCategory = category.categoryInApi ?: return@observeForever
                val task = client.getIssuesAsync(apiCategory)
                task.invokeOnCompletion { e ->
                    if (e == null) {
                        postValue(task.getCompleted())
                    }
                }
            }
        }
    }

    /** タグ一覧の取得を通知する */
    @OptIn(ExperimentalCoroutinesApi::class)
    inner class TagsLiveData(
        private var user: String? = null
    ) : LiveData<List<Tag>>() {
        override fun onActive() {
            load()
        }

        /** ユーザーを設定してタグリストをリロードする */
        fun setUser(user: String) {
            this.user = user
            load()
        }

        private fun load() {
            val user = user ?: client.account?.name ?: return
            val task = client.getUserTagsAsync(user)
            task.invokeOnCompletion { e ->
                if (e == null) {
                    postValue(task.getCompleted())
                }
            }
        }
    }
}
