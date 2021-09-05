package com.suihan74.satena.scenes.entries2

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.suihan74.hatenaLib.*
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.*
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepository
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.checkFromSpam
import kotlinx.coroutines.*
import org.threeten.bp.LocalDateTime

/** エントリリストの取得時にカテゴリによっては必要な必要な追加パラメータ */
class LoadEntryParameter {
    companion object {
        /** 検索クエリ(String) : Category.Search */
        const val SEARCH_QUERY = "LoadEntryParameter.SEARCH_QUERY"

        /** 検索タイプ(SearchType: TAG or TEXT) : Category.Search */
        const val SEARCH_TYPE = "LoadEntryParameter.SEARCH_TYPE"

        /** サイトURL(String) : Category.Site */
        const val SITE_URL = "LoadEntryParameter.SITE_URL"

        /** ページ(Int) : Category.Site */
        const val PAGE = "LoadEntryParameter.PAGE"

        /** タグ : Category.MyBookmarks */
        const val TAG = "LoadEntryParameter.TAG"

        /** ユーザー名 : Category.User */
        const val USER = "LoadEntryParameter.USER"

        /** ユーザーエントリを取得するか : Category.Memorial15th */
        const val IS_USER = "LoadEntryParameter.IS_USER"
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

/**
 * ブコメ単体で取得するために必要な情報
 *
 * スター -> ブコメの変換用一時データ
 */
private data class BookmarkCommentUrl (
    val url : String,
    val user : String,
    val timestamp : String,
    val eid : Long,
    val starsCount: List<Star>
)

// ------ //

class EntriesRepository(
    private val context: Context,
    private val client: HatenaClient,
    private val accountLoader: AccountLoader,
    val ignoredEntriesRepo: IgnoredEntriesRepository,
    val favoriteSitesRepo: FavoriteSitesRepository
) {
    /** アプリ内アップデート */
    private var appUpdateManager: AppUpdateManager? = null

    /** 設定 */
    private val prefs by lazy {
        SafeSharedPreferences.create<PreferenceKey>(context)
    }

    /** 閲覧履歴 */
    private val historyPrefs by lazy {
        SafeSharedPreferences.create<EntriesHistoryKey>(context)
    }

    /** サインイン状態 */
    val signedIn : Boolean
        get() = client.signedIn()

    val signedInLiveData = SignedInLiveData()

    /** ドロワ位置 */
    val drawerGravity : Int
        get() = prefs.getInt(PreferenceKey.DRAWER_GRAVITY)

    /** ホームカテゴリ */
    val homeCategory : Category
        get() = Category.fromId(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))

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
        get() = TapEntryAction.fromId(prefs.getInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION))

    /** エントリ項目複数回クリック時の挙動 */
    val entryMultipleClickedAction : TapEntryAction
        get() = TapEntryAction.fromId(prefs.getInt(PreferenceKey.ENTRY_MULTIPLE_TAP_ACTION))

    /** エントリ項目長押し時の挙動 */
    val entryLongClickedAction : TapEntryAction
        get() = TapEntryAction.fromId(prefs.getInt(PreferenceKey.ENTRY_LONG_TAP_ACTION))

    /** エントリ右端クリック時の挙動 */
    val entryEdgeClickedAction : TapEntryAction
        get() = TapEntryAction.fromId(prefs.getInt(PreferenceKey.ENTRY_EDGE_SINGLE_TAP_ACTION))

    /** エントリ右端複数回クリック時の挙動 */
    val entryEdgeMultipleClickedAction : TapEntryAction
        get() = TapEntryAction.fromId(prefs.getInt(PreferenceKey.ENTRY_EDGE_MULTIPLE_TAP_ACTION))

    /** エントリ右端長押し時の挙動 */
    val entryEdgeLongClickedAction : TapEntryAction
        get() = TapEntryAction.fromId(prefs.getInt(PreferenceKey.ENTRY_EDGE_LONG_TAP_ACTION))

    /** エントリ項目クリック回数判定時間 */
    val entryMultipleClickDuration: Long
        get() = prefs.getLong(PreferenceKey.ENTRY_MULTIPLE_TAP_DURATION)

    /** アプリ終了前に確認する */
    val isTerminationDialogEnabled : Boolean
        get() = prefs.getBoolean(PreferenceKey.USING_TERMINATION_DIALOG)

    /** ボタン類を画面下部に集約する */
    val isBottomLayoutMode : Boolean
        get() = prefs.getBoolean(PreferenceKey.ENTRIES_BOTTOM_LAYOUT_MODE)

    /** スクロールにあわせて下部バーを隠す */
    val hideBottomAppBarByScroll : Boolean
        get() = prefs.getBoolean(PreferenceKey.ENTRIES_HIDE_BOTTOM_LAYOUT_BY_SCROLLING)

    /** タブページャのスワイプ感度 */
    val pagerScrollSensitivity : Float
        get() = prefs.getFloat(PreferenceKey.ENTRIES_PAGER_SCROLL_SENSITIVITY)

    /** カテゴリリストの表示形式 */
    val categoriesMode : CategoriesMode
        get() = CategoriesMode.fromOrdinal(prefs.getInt(PreferenceKey.ENTRIES_CATEGORIES_MODE))

    /** ボトムバーの項目 */
    val bottomBarItems : List<UserBottomItem>
        get() = prefs.get(PreferenceKey.ENTRIES_BOTTOM_ITEMS)

    /** ボトムバーの項目の配置方法 */
    val bottomBarItemsGravity : Int
        get() = prefs.get(PreferenceKey.ENTRIES_BOTTOM_ITEMS_GRAVITY)

    /** ボトムバーの追加項目の配置方法 */
    val extraBottomItemsAlignment : ExtraBottomItemsAlignment
        get() = ExtraBottomItemsAlignment.fromId(prefs.getInt(PreferenceKey.ENTRIES_EXTRA_BOTTOM_ITEMS_ALIGNMENT))

    /** エクストラスクロール機能のツマミの配置 */
    val extraScrollingAlignment
        get() = ExtraScrollingAlignment.fromId(prefs.getInt(PreferenceKey.ENTRIES_EXTRA_SCROLL_ALIGNMENT))

    /** 初期化処理 */
    suspend fun initialize(forceUpdate: Boolean = false) = withContext(Dispatchers.Default) {
        accountLoader.signInAccounts(forceUpdate)
        signedInLiveData.post(client.signedIn())
        categoriesLiveData.post(client.signedIn())
        ignoredEntriesRepo.loadIgnoredEntriesForEntries()
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

            Category.MyBookmarks -> loadMyBookmarks(offset, params)

            Category.Search -> searchEntries(tabPosition, offset, params!!)

            Category.Stars ->
                if (tabPosition == 0) loadMyStars()
                else loadStarsReport()

            Category.User -> loadUserEntries(offset, params!!)

            Category.Memorial15th -> loadHistoricalEntries(tabPosition, params)

            Category.Followings -> loadFollowingsEntries(offset)

            Category.FavoriteSites -> {
                val page = params?.get<Int>(LoadEntryParameter.PAGE) ?: 0
                loadFavoriteSitesEntries(tabPosition, page)
            }

            else -> throw NotImplementedError("refreshing \"${category.name}\" is not implemented")
        }

    /** はてなの通常のエントリーリストを取得する */
    private suspend fun loadHatenaEntries(tabPosition: Int, category: com.suihan74.hatenaLib.Category, offset: Int?) : List<Entry> {
        val entriesType = EntriesType.fromId(tabPosition)
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
    private suspend fun loadMyBookmarks(offset: Int?, params: LoadEntryParameter?) : List<Entry> {
        val tag = params?.get<String>(LoadEntryParameter.TAG)
        val query = params?.get<String>(LoadEntryParameter.SEARCH_QUERY)

        val isQueryEnabled = !query.isNullOrBlank()
        val isTagEnabled = !tag.isNullOrBlank()

        return when {
            isQueryEnabled && isTagEnabled -> {
                val queries = Regex("""\s+""").split(query!!)
                val regex = Regex(queries.joinToString(separator = "|") { Regex.escape(it) })

                val entries = client.searchMyEntriesAsync(tag!!, SearchType.Tag, of = offset).await()
                entries.filter {
                    regex.containsMatchIn(it.title) || regex.containsMatchIn(it.description) || regex.containsMatchIn(it.bookmarkedData?.commentRaw ?: "")
                }
            }

            isQueryEnabled ->
                client.searchMyEntriesAsync(query!!, SearchType.Text, of = offset).await()

            isTagEnabled ->
                client.searchMyEntriesAsync(tag!!, SearchType.Tag, of = offset).await()

            else ->
                client.getMyBookmarkedEntriesAsync(of = offset).await()
        }
    }

    /** ユーザーがブクマしたエントリ一覧を取得する */
    private suspend fun loadUserEntries(offset: Int?, params: LoadEntryParameter) : List<Entry> {
        val user = params.get<String>(LoadEntryParameter.USER)!!
        val tag = params.get<String>(LoadEntryParameter.TAG)

        return client.getUserEntriesAsync(user = user, tag = tag, of = offset).await()
    }

    /** 通知リストを取得する */
    suspend fun loadNotices() : List<Notice> = coroutineScope {
        val fetchedNotices = client.getNoticesAsync().await().notices

        // 通知の既読状態を更新
        val lastSeenUpdatable = prefs.getBoolean(PreferenceKey.NOTICES_LAST_SEEN_UPDATABLE)
        if (lastSeenUpdatable) {
            try {
                client.updateNoticesLastSeenAsync().await()
            }
            catch (e: Throwable) {
            }
        }
        prefs.edit {
            putObject(PreferenceKey.NOTICES_LAST_SEEN, LocalDateTime.now())
        }

        val noticesPrefs = SafeSharedPreferences.create<NoticesKey>(context, NoticesKey.fileName(client.account!!.name))
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

        // コメント情報がない通知のブコメを取得する
        val fixNoticesTasks = notices.map { notice ->
            async {
                runCatching {
                    if (notice.verb == Notice.VERB_STAR && notice.metadata?.subjectTitle.isNullOrBlank()) {
                        val md = NoticeMetadata(
                            client.getBookmarkPageAsync(
                                notice.eid,
                                notice.user
                            ).await().comment.body
                        )
                        notice.copy(metadata = md)
                    }
                    else notice
                }.getOrDefault(notice)
            }
        }
        fixNoticesTasks.awaitAll()
        val fixedNotices = fixNoticesTasks.map { it.await() }

        noticesPrefs.edit {
            put(NoticesKey.NOTICES, fixedNotices)

            // 古い削除指定を消去する
            if (oldestMatched < LocalDateTime.MAX) {
                put(NoticesKey.REMOVED_NOTICE_TIMESTAMPS, removedNotices.filter {
                    it.modified >= oldestMatched
                })
            }
        }

        return@coroutineScope fixedNotices
    }

    /** 障害情報を取得する */
    suspend fun loadInformation() : List<MaintenanceEntry> {
        return client.getMaintenanceEntriesAsync().await()
    }

    /** 最近つけたスターを取得する */
    private suspend fun loadMyStars() : List<Entry> {
        val starsEntries = client.getRecentStarsAsync().await()
        return convertStarsToEntries(starsEntries)
    }

    /** 最近つけられたスターを取得する */
    private suspend fun loadStarsReport() : List<Entry> {
        val starsEntries = client.getRecentStarsReportAsync().await()
        return convertStarsToEntries(starsEntries)
    }

    /** スター情報をエントリーリストに変換する */
    private suspend fun convertStarsToEntries(starsEntries: List<StarsEntry>) : List<Entry> {
        val urlRegex = Regex("""https?://b\.hatena\.ne\.jp/(.+)/(\d+)#bookmark-(\d+)""")
        val data = starsEntries
            .mapNotNull {
                val match = urlRegex.matchEntire(it.url) ?: return@mapNotNull null
                val user = match.groups[1]?.value ?: return@mapNotNull null
                val timestamp = match.groups[2]?.value ?: return@mapNotNull null
                val eid = match.groups[3]?.value ?: return@mapNotNull null
                val starsCount = it.allStars
                BookmarkCommentUrl(
                    it.url,
                    user,
                    timestamp,
                    eid.toLong(),
                    starsCount
                )
            }
            .groupBy { it.eid.toString() + "_" + it.user }
            .map {
                val starsCount = it.value
                    .flatMap { e -> e.starsCount }
                    .groupBy { s -> s.color }
                    .map { s -> Star("", "", s.key, s.value.size) }
                it.value.first().copy(
                    starsCount = starsCount
                )
            }

        // ブクマ自体のページを取得する
        var tasks: List<Deferred<BookmarkPage?>>? = null
        val myBookmarks = HashMap<Long, Deferred<BookmarkPage?>>()
        coroutineScope {
            tasks = data.map { async {
                try {
                    client.getBookmarkPageAsync(it.eid, it.user).await()
                }
                catch (e: Throwable) {
                    Log.i("removed", Log.getStackTraceString(e))
                    null
                }
            } }

            // 自分のブクマを取得する(StarReportでは無視)
            val accountUser = client.account?.name ?: ""
            data
                .groupBy { it.eid }
                .forEach {
                    if (myBookmarks.containsKey(it.key)) return@forEach
                    val task = async {
                        if (it.value.any { b -> b.user == accountUser }) null
                        else {
                            try {
                                client.getBookmarkPageAsync(it.key, accountUser).await()
                            }
                            catch (e: Throwable) {
                                Log.i("not_bookmarked", Log.getStackTraceString(e))
                                null
                            }
                        }
                    }
                    myBookmarks[it.key] = task
                }
        }

        return (tasks ?: emptyList())
            .mapIndexedNotNull { index, deferred ->
                if (deferred.isCancelled) return@mapIndexedNotNull null

                val eid = data[index].eid
                try {
                    val bookmark = deferred.await() ?: return@mapIndexedNotNull null
                    val bookmarkedData = BookmarkResult(
                        user = bookmark.user,
                        comment = bookmark.comment.body,
                        tags = bookmark.comment.tags,
                        timestamp = bookmark.timestamp,
                        userIconUrl = client.getUserIconUrl(bookmark.user),
                        commentRaw = bookmark.comment.raw,
                        permalink = data[index].url,
                        success = true,
                        private = false,
                        eid = eid,
                        starsCount = data[index].starsCount
                    )
                    if (bookmark.user == client.account?.name) {
                        bookmark.entry.copy(
                            rootUrl = Uri.parse(bookmark.entry.url)?.encodedPath ?: bookmark.entry.url,
                            bookmarkedData = bookmarkedData
                        )
                    }
                    else {
                        bookmark.entry.copy(
                            rootUrl = Uri.parse(bookmark.entry.url)?.encodedPath ?: bookmark.entry.url,
                            myhotentryComments = listOf(bookmarkedData)
                        )
                    }
                }
                catch (e: Throwable) {
                    Log.i("test", Log.getStackTraceString(e))
                    null
                }
            }
            .groupBy { it.url }
            .map {
                val entries = it.value
                val entry = entries.first()
                entry.copy(
                    bookmarkedData = entry.bookmarkedData ?: myBookmarks[entry.id]?.await()?.let { my ->
                        BookmarkResult(
                            user = my.user,
                            comment = my.comment.body,
                            tags = my.comment.tags,
                            timestamp = my.timestamp,
                            userIconUrl = client.getUserIconUrl(my.user),
                            commentRaw = my.comment.raw,
                            permalink = client.getBookmarkCommentUrl(entry.id, my.user),
                            success = true,
                            private = false,
                            eid = entry.id,
                            starsCount = null
                        )
                    },
                    myhotentryComments = entries.flatMap { it.myHotEntryComments.orEmpty() }
                )
            }
    }

    /** エントリを検索する */
    private suspend fun searchEntries(tabPosition: Int, offset: Int?, params: LoadEntryParameter) : List<Entry> {
        val query = params.get<String>(LoadEntryParameter.SEARCH_QUERY)!!
        val searchType = params.get<SearchType>(LoadEntryParameter.SEARCH_TYPE)!!
        val entriesType = EntriesType.fromId(tabPosition)

        return client.searchEntriesAsync(
            query = query,
            searchType = searchType,
            entriesType = entriesType,
            of = offset
        ).await()
    }

    /** 最新のエントリーリストを読み込む(Issue指定) */
    private suspend fun loadEntries(issue: Issue, tabPosition: Int, offset: Int? = null) : List<Entry> {
        val entriesType = EntriesType.fromId(tabPosition)
        return client.getEntriesAsync(
            entriesType = entriesType,
            issue = issue,
            of = offset
        ).await()
    }

    /** 指定したサイトのエントリーリストを読み込む */
    private suspend fun loadSiteEntries(url: String, tabPosition: Int, page: Int? = null) : List<Entry> {
        val entriesType = EntriesType.fromId(tabPosition)
        return client.getEntriesAsync(
            url = url,
            entriesType = entriesType,
            allMode = true,
            page = page ?: 0
        ).await()
    }

    /** はてなの歴史エントリを取得する */
    private suspend fun loadHistoricalEntries(tabPosition: Int, params: LoadEntryParameter?) : List<Entry> =
        if (params?.get<Boolean>(LoadEntryParameter.IS_USER) == true)
            client.getUserHistoricalEntriesAsync(2005 + tabPosition, 30).await()
        else
            client.getHistoricalEntriesAsync(2005 + tabPosition).await()

    /** お気に入りユーザーのエントリリストを読み込む */
    private suspend fun loadFollowingsEntries(offset: Int? = null) : List<Entry> {
        val bookmarks = client.getFollowingBookmarksAsync(offset = offset).await()
        return bookmarks.map { it.toEntry() }
    }

    /** お気に入りサイトのエントリリストを読み込む */
    private suspend fun loadFavoriteSitesEntries(tabPosition: Int, page: Int? = null) : List<Entry> {
        val entriesType = EntriesType.fromId(tabPosition)
        val sites = favoriteSitesRepo.favoriteSites.value ?: emptyList()

        val tasks = sites
            .filter { it.isEnabled }
            .map { site -> client.async {
                try {
                    client.getEntriesAsync(
                        url = site.url,
                        entriesType = entriesType,
                        allMode = true,
                        page = page ?: 0
                    ).await()
                }
                catch (e: Throwable) {
                    emptyList()
                }
            } }

        val entries = tasks.awaitAll().flatten()

        return when (tabPosition) {
            0 -> entries.sortedByDescending { it.count }
            1 -> entries.sortedByDescending { it.date }
            else -> throw IndexOutOfBoundsException("tabPosition is out of bounds: pos = $tabPosition")
        }
    }

    // ------ //

    /** エントリを「あとで読む」タグをつけてブクマする */
    suspend fun readLaterEntry(entry: Entry) : BookmarkResult =
        client.postBookmarkAsync(
            url = entry.url,
            readLater = true
        ).await()

    /** 「あとで読む」エントリを読んだ */
    suspend fun readEntry(entry: Entry) : Pair<EntryReadActionType, BookmarkResult?> {
        val action = EntryReadActionType.fromOrdinal(prefs.getInt(PreferenceKey.ENTRY_READ_ACTION_TYPE))

        return action to when (action) {
            EntryReadActionType.REMOVE -> {
                deleteBookmark(entry)
                null
            }

            EntryReadActionType.DIALOG -> null

            else -> {
                val comment = when (action) {
                    EntryReadActionType.SILENT_BOOKMARK -> ""
                    EntryReadActionType.READ_TAG -> "[読んだ]"
                    EntryReadActionType.BOILERPLATE -> prefs.getString(PreferenceKey.ENTRY_READ_ACTION_BOILERPLATE) ?: ""
                    else -> ""
                }

                client.postBookmarkAsync(
                    url = entry.url,
                    readLater = false,
                    comment = comment
                ).await()
            }
        }
    }

    /** ブクマを削除する */
    suspend fun deleteBookmark(entry: Entry) {
        client.deleteBookmarkAsync(entry.url).await()
    }

    /** 通知を削除する */
    fun deleteNotice(notice: Notice) {
        val prefs = SafeSharedPreferences.create<NoticesKey>(context, NoticesKey.fileName(client.account!!.name))
        val removedNotices = prefs.get<List<NoticeTimestamp>>(NoticesKey.REMOVED_NOTICE_TIMESTAMPS)
            .plus(NoticeTimestamp(notice.created, notice.modified))

        prefs.edit {
            put(NoticesKey.REMOVED_NOTICE_TIMESTAMPS, removedNotices)
        }
    }

    // ------ //

    /** エントリをフィルタリングする */
    suspend fun filterEntries(entries: List<Entry>) : List<Entry> = withContext(Dispatchers.IO) {
        return@withContext entries.filterNot { entry ->
            ignoredEntriesRepo.ignoredEntriesForEntries.value?.any { it.isMatched(entry) } == true
        }
    }

    /** アプリ内アップデートを使用する */
    fun startAppUpdateManager(listener: (AppUpdateInfo)->Unit) {
        when (AppUpdateNoticeMode.fromId(prefs.getInt(PreferenceKey.APP_UPDATE_NOTICE_MODE))) {
            AppUpdateNoticeMode.NONE -> {
                this.appUpdateManager = null
            }

            else -> {
                if (appUpdateManager == null) {
                    appUpdateManager = AppUpdateManagerFactory.create(context)
                }

                // アップデートを確認する
                appUpdateManager?.appUpdateInfo?.addOnSuccessListener { info ->
                    when (info.updateAvailability()) {
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                            listener(info)

                        UpdateAvailability.UPDATE_AVAILABLE ->
                            handleUpdateAvailable(info, listener)

                        else -> {}
                    }
                }
            }
        }
    }

    /** アップデートできる場合に、まず通知するべきかどうかを判別する */
    private fun handleUpdateAvailable(info: AppUpdateInfo, listener: (AppUpdateInfo)->Unit) {
        val app = SatenaApplication.instance
        val latestVersion = info.availableVersionCode().toLong()

        if (!prefs.getBoolean(PreferenceKey.NOTICE_IGNORED_APP_UPDATE)) {
            // 一度無視したアップデートを二度と通知しない設定が有効な場合
            val lastNoticedVersion = prefs.getLong(PreferenceKey.LAST_NOTICED_APP_UPDATE_VERSION)
            if (lastNoticedVersion == latestVersion) {
                return
            }
        }

        // 通知済みの最新バージョンを更新する
        prefs.edit {
            putLong(PreferenceKey.LAST_NOTICED_APP_UPDATE_VERSION, latestVersion)
        }

        when (AppUpdateNoticeMode.fromId(prefs.getInt(PreferenceKey.APP_UPDATE_NOTICE_MODE))) {
            // 機能追加アップデートだけを通知する
            AppUpdateNoticeMode.ADD_FEATURES -> {
                val currentMajor = app.majorVersionCode
                val currentMinor = app.minorVersionCode

                val latestMajor = app.getMajorVersion(latestVersion)
                val latestMinor = app.getMinorVersion(latestVersion)

                if (latestMajor > currentMajor || latestMinor > currentMinor) {
                    listener(info)
                }
            }

            // 全てのアップデートを通知する
            else -> listener(info)
        }
    }

    /** アプリのアップデートを開始する */
    fun resumeAppUpdate(activity: Activity, info: AppUpdateInfo, requestCode: Int) {
        appUpdateManager?.startUpdateFlowForResult(
            info,
            AppUpdateType.IMMEDIATE,
            activity,
            requestCode
        )
    }

    /** サインイン状態の変更を通知する */
    inner class SignedInLiveData : LiveData<Boolean>(signedIn) {
        internal fun post(b: Boolean?) {
            postValue(b ?: false)
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

    // ------ //

    val changeHomeByLongTap : Boolean
        get() = prefs.getBoolean(PreferenceKey.ENTRIES_CHANGE_HOME_BY_LONG_TAPPING_TAB)

    /**
     * ホームカテゴリを更新する
     */
    fun updateHomeCategory(category: Category) {
        prefs.edit {
            put(PreferenceKey.ENTRIES_HOME_CATEGORY, category.id)
        }
    }

    /**
     * カテゴリの初期表示タブを更新する
     */
    fun updateDefaultTab(category: Category, tabPosition: Int) {
        val key = PreferenceKey.ENTRIES_DEFAULT_TABS
        val settings = prefs.getObject<EntriesDefaultTabSettings>(key) ?: EntriesDefaultTabSettings()
        settings[category] = tabPosition
        prefs.edit {
            putObject(key, settings)
        }
    }
}
