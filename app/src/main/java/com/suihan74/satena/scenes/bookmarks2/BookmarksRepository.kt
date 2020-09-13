package com.suihan74.satena.scenes.bookmarks2

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.suihan74.hatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.OnError
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.suihan74.utilities.lock
import kotlinx.coroutines.*

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksRepository(
    private val client: HatenaClient,
    private val accountLoader: AccountLoader,
    private val prefs: SafeSharedPreferences<PreferenceKey>
) {
    /** エントリ情報が正しく設定されているか */
    val isInitialized : Boolean
        get() = _entry != null

    /** エントリ情報 */
    var entry: Entry
        get() = _entry!!
        private set(value) {
            _entry = value
        }

    private var _entry: Entry? = null

    /** ブクマエントリ */
    var bookmarksEntry: BookmarksEntry? = null
        private set

    /** 人気ブクマリストを含む概要データのキャッシュ */
    var bookmarksDigest: BookmarksDigest? = null
        private set

    /** 人気ブクマリストのキャッシュ */
    var bookmarksPopular: List<Bookmark> = emptyList()
        private set

    /** 新着ブクマリストのキャッシュ */
    var bookmarksRecent: List<Bookmark> = emptyList()
        get() = synchronized(field) { field }
        private set(value) {
            synchronized(field) {
                field = value
            }
        }

    /** 非表示ユーザーIDリストのキャッシュ */
    var ignoredUsers: List<String> = emptyList()
        private set

    /** スター情報のキャッシュ -> key = 対象ユーザー名, value = スター情報 */
    private val starsMap = HashMap<String, StarsEntry>()

    /** ログイン状態 */
    val signedIn
        get() = client.signedIn()

    /** ログイン中のユーザー名 */
    val userSignedIn
        get() = client.account?.name

    /** テーマ */
    val themeId: Int by lazy {
        if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppTheme_Dark
        else R.style.AppTheme_Light
    }

    /** スクロールでツールバーを隠す */
    val hideToolbarByScrolling: Boolean by lazy {
        prefs.getBoolean(PreferenceKey.BOOKMARKS_HIDING_TOOLBAR_BY_SCROLLING)
    }

    /** スクロールで下部ボタンを隠す */
    val hideButtonsByScrolling: Boolean by lazy {
        prefs.getBoolean(PreferenceKey.BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING)
    }

    /** 表示ユーザーリストを監視するライブデータを生成する */
    val ignoredUsersLiveData by lazy {
        IgnoredUsersLiveData(this)
    }

    /** サインインしているユーザーの所持スター情報を監視するライブデータを作成する */
    val userStarsLiveData by lazy {
        UserStarsLiveData(client)
    }

    /** エントリ中のブクマについたすべてのスター取得を監視するライブデータを作成する */
    val allStarsLiveData by lazy {
        StarsLiveData(
            client,
            entry,
            this
        )
    }

    /** スター送信前に確認する */
    val usePostStarDialog by lazy {
        prefs.getBoolean(PreferenceKey.USING_POST_STAR_DIALOG)
    }

    /** リンクをシングルタップしたときの処理 */
    val linkSingleTapAction : TapEntryAction by lazy {
        TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION))
    }

    /** リンクをロングタップしたときの処理 */
    val linkLongTapAction : TapEntryAction by lazy {
        TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION))
    }

    /** リポジトリ初期化 */
    suspend fun init() {
        accountLoader.signInAccounts()
    }

    /** ユーザーに付けられたスター情報を取得する */
    fun getStarsEntryTo(user: String) = lock(starsMap) { starsMap[user] }
    /** ユーザーに付けられたスター情報を登録or更新する */
    private fun setStarsEntryTo(user: String, entry: StarsEntry) = lock(starsMap) { starsMap[user] = entry }

    /** ユーザーが付けたスター情報を取得する */
    fun getStarsEntryFrom(user: String) = lock(starsMap) {
        starsMap.mapNotNull m@ {
            if (it.value.allStars.any { star -> star.user == user }) it.value
            else null
        }
    }

    /** エントリ情報を取得 */
    suspend fun loadEntry(url: String) {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            throw InvalidUrlException(url)
        }

        val modifiedUrl = modifySpecificUrls(url)!!
        entry = client.getEntryAsync(modifiedUrl).await()
    }

    /** エントリ情報を取得 */
    suspend fun loadEntry(eid: Long) {
        entry = client.getEntryAsync(eid).await()
    }

    /** 既にロード済みのエントリ情報をリポジトリにセットする */
    suspend fun loadEntry(entry: Entry) {
        if (entry.id == 0L) {
            val eid = client.getEntryIdAsync(entry.url).await()
            this.entry = entry.copy(id = eid ?: 0)
        }
        else {
            this.entry = entry
        }
    }

    /** ブックマークエントリを取得 */
    fun loadBookmarksEntryAsync() =
        client.getBookmarksEntryAsync(entry.url).apply {
            invokeOnCompletion { e ->
                if (e != null) return@invokeOnCompletion

                val result = getCompleted()
                bookmarksEntry = result
                if (bookmarksEntry == null) return@invokeOnCompletion

                allStarsLiveData
            }
        }

    /** 人気ブクマなどの概要情報を取得 */
    fun loadBookmarksDigestAsync() =
        client.getDigestBookmarksAsync(entry.url).apply {
            invokeOnCompletion { e ->
                if (e == null) {
                    val result = getCompleted()
                    bookmarksDigest = result
                    bookmarksPopular = result.scoredBookmarks.map { Bookmark.create(it) }
                }
            }
        }

    /** 追加ロード用のカーソル */
    private val recentBookmarksCursorLock by lazy { Any() }
    private var recentBookmarksCursor: String? = null
        get() = synchronized(recentBookmarksCursorLock) { field }
        set(value) {
            synchronized(recentBookmarksCursorLock) { field = value }
        }

    val additionalLoadable: Boolean
        get() = recentBookmarksCursor != null

    /** 新着ブクマを取得済みの部分に達するまで取得する */
    private fun loadMostRecentBookmarksAsync(url: String) : Deferred<BookmarksWithCursor> {
        val latestBookmark = bookmarksRecent.firstOrNull()

        return if (latestBookmark == null) {
            client.getRecentBookmarksAsync(url = url)
        }
        else {
            client.async(Dispatchers.Default) {
                var cursor: String? = null
                val bookmarks = ArrayList<BookmarkWithStarCount>()
                while (true) {
                    val response = try {
                        client.getRecentBookmarksAsync(url = url, cursor = cursor).await()
                    }
                    catch (e: Throwable) {
                        break
                    }

                    cursor = response.cursor
                    bookmarks.addAll(response.bookmarks)

                    if (cursor == null || response.bookmarks.any { it.timestamp <= latestBookmark.timestamp }) {
                        break
                    }
                }

                return@async BookmarksWithCursor(cursor = cursor, bookmarks = bookmarks)
            }
        }
    }

    /** 新着ブクマを取得 */
    fun loadBookmarksRecentAsync(
        additionalLoading: Boolean = false
    ) : Deferred<List<Bookmark>> = client.async(Dispatchers.Default) {
        if (additionalLoading && recentBookmarksCursor == null) return@async emptyList()

        var cursor: String? = null
        val page = ArrayList<Bookmark>()

        val response =
            if (!additionalLoading) loadMostRecentBookmarksAsync(url = entry.url).await()
            else client.getRecentBookmarksAsync(
                    url = entry.url,
                    cursor = recentBookmarksCursor
                ).await()

        cursor = response.cursor
        page.addAll(response.bookmarks.map { Bookmark.create(it) })

        // 有言ブクマを一定数確保するために繰り返し続きをロードする
        while (cursor != null && page.count { it.comment.isNotBlank() } < 10) {
            val r = client.getRecentBookmarksAsync(
                url = entry.url,
                cursor = cursor
            ).await()

            cursor = r.cursor
            page.addAll(r.bookmarks.map { Bookmark.create(it) })
        }

        // 追加ロード用のカーソルを更新する
        if (page.isEmpty()) {
            recentBookmarksCursor = null
        }
        else if (bookmarksRecent.isEmpty() || page.last().timestamp <= bookmarksRecent.last().timestamp) {
            recentBookmarksCursor = cursor
        }

        bookmarksRecent = page
            .plus(
                bookmarksRecent.filterNot {
                    page.any { updated -> it.user == updated.user }
                }
            )
            .sortedByDescending { it.timestamp }

        // エントリ情報のブクマリストにも追加する
        bookmarksEntry?.let { bEntry ->
            val newBookmarks = page.filterNot {
                bEntry.bookmarks.any { updated -> it.user == updated.user }
            }

            if (newBookmarks.isNotEmpty()) {
                bookmarksEntry = bEntry.copy(
                    bookmarks = bEntry.bookmarks
                        .plus(newBookmarks)
                        .sortedByDescending { it.timestamp }
                )
            }
        }

        // 追加分のスターを読み込む
        if (!allStarsLiveData.loading) {
            allStarsLiveData.updateAsync().start()
        }

        return@async page
    }

    /** 新着ブクマリストを追加ロードする */
    fun loadNextBookmarksRecentAsync() = loadBookmarksRecentAsync(true)

    /** 非表示ユーザーのリストをロードする */
    fun loadIgnoredUsersAsync(
        forceUpdate: Boolean = false
    ) : Deferred<List<String>> = client.async(Dispatchers.Default) {
        try {
            val old = ignoredUsers
            val new = client.getIgnoredUsersAsync(forceUpdate).await()
            ignoredUsers = new

            if (forceUpdate || new.size != old.size || !new.containsAll(old) || !old.containsAll(new)) {
                ignoredUsersLiveData.notifyPost()
            }
        }
        catch (e: Throwable) {
            throw e
        }

        return@async ignoredUsers
    }

    /** ユーザーを非表示にする */
    fun ignoreUserAsync(user: String) =
        client.ignoreUserAsync(user).apply {
            invokeOnCompletion { e ->
                if (e == null) {
                    ignoredUsers = ignoredUsers.plus(user)
                    ignoredUsersLiveData.notifyPost()
                }
            }
        }

    /** ユーザーの非表示を解除する */
    fun unIgnoreUserAsync(user: String) =
        client.unignoreUserAsync(user).apply {
            invokeOnCompletion { e ->
                if (e == null) {
                    ignoredUsers = ignoredUsers.minus(user)
                    ignoredUsersLiveData.notifyPost()
                }
            }
        }

    /** ブクマにスターをつける */
    suspend fun postStar(bookmark: Bookmark, color: StarColor, quote: String = "") : Star {
        return client.postStarAsync(bookmark.getBookmarkUrl(entry), color, quote).await()
    }

    /** スターを削除する */
    suspend fun deleteStar(bookmark: Bookmark, star: Star) {
        client.deleteStarAsync(bookmark.getBookmarkUrl(entry), star).await()
    }

    /** ブクマに付けられたスター取得を監視するライブデータを生成する */
    fun createStarsEntryLiveData(bookmark: Bookmark) =
        StarsEntryLiveData(
            client,
            entry,
            bookmark,
            this
        ).also { liveData ->
            var observer: Observer<List<StarsEntry>>? = null
            var initialized = false
            observer = Observer<List<StarsEntry>> { all ->
                if (initialized && !liveData.hasActiveObservers()) {
                    allStarsLiveData.removeObserver(observer!!)
                    return@Observer
                }
                initialized = true

                val url = bookmark.getBookmarkUrl(entry)
                if (all.any { it.url == url }) {
                    liveData.notifyReload()
                }
            }

            allStarsLiveData.observeForever(observer)
        }

    /** ブクマを通報 */
    suspend fun reportBookmark(report: Report) {
        client.reportAsync(report).await()
    }

    /** ユーザーのブクマを取得する */
    val userBookmark : Bookmark? get() =
        if (!signedIn) null
        else {
            bookmarksEntry?.bookmarks?.firstOrNull { it.user == userSignedIn }
                ?: entry.bookmarkedData?.let { Bookmark.create(it) }
        }

    /**
     * ユーザーのブクマを更新する
     */
    suspend fun updateUserBookmark(bookmarkResult: BookmarkResult) = withContext(Dispatchers.Default) {
        val bookmark = Bookmark.create(bookmarkResult)
        loadEntry(entry.copy(bookmarkedData =  bookmarkResult))
        if (bookmarksEntry != null) {
            val bEntry = bookmarksEntry!!
            bookmarksEntry = bEntry.copy(bookmarks = bEntry.bookmarks.map {
                if (it.user == bookmark.user) bookmark
                else it
            })
        }

        bookmarksRecent = bookmarksRecent.map {
            if (it.user == bookmark.user) bookmark
            else it
        }

        if (bookmarksDigest != null) {
            val bDigest = bookmarksDigest!!
            bookmarksDigest = bDigest.copy(scoredBookmarks = bDigest.scoredBookmarks.map {
                if (it.user == bookmark.user) it.copy(comment = bookmark.comment, tags = bookmark.tags, timestamp = bookmark.timestamp)
                else it
            })
        }
        bookmarksPopular = bookmarksPopular.map {
            if (it.user == bookmark.user) bookmark
            else it
        }
    }

// ------ //

    class IgnoredUsersLiveData(
        private val repository: BookmarksRepository
    ) : LiveData<List<String>>() {
        override fun getValue() = repository.ignoredUsers

        fun notifyPost() {
            postValue(repository.ignoredUsers)
        }
    }


// ------ //

    /** ユーザーの所持スターを監視するライブデータ */
    class UserStarsLiveData(
        private val client: HatenaClient
    ) : LiveData<UserColorStarsCount>() {
        var userStars : UserColorStarsCount? = null
            private set

        override fun getValue() = userStars

        var loaded = false
            get() = synchronized(field) { field }
            private set(value) {
                synchronized(field) {
                    field = value
                }
            }

        suspend fun load() {
            loaded = false
            if (client.signedIn()) {
                try {
                    val result = client.getMyColorStarsAsync().await()
                    userStars = result
                    loaded = true
                    postValue(result)
                }
                catch (e: Throwable) {
                    setDummy()
                    throw e
                }
            }
            else {
                setDummy()
            }
        }

        private fun setDummy() {
            val dummy = UserColorStarsCount(0, 0, 0, 0)
            userStars = dummy
            postValue(dummy)
        }
    }

// ------ //

    /** エントリ中の全スター情報取得を監視するライブデータ */
    class StarsLiveData(
        private val client: HatenaClient,
        private val entry: Entry,
        private val repository: BookmarksRepository
    ) : LiveData<List<StarsEntry>>(emptyList()) {
        private var task: Deferred<List<StarsEntry>>? = null
            get() = lock(this) { field }
            private set(value) {
                lock(this) { field = value }
            }

        /** ロード中か否か */
        val loading: Boolean
            get() = task != null

        /** 再通知 */
        fun notifyReload() {
            postValue(value)
        }

        override fun onActive() {
            updateAsync().start()
        }

        override fun onInactive() {
            task?.cancel()
            task = null
        }

        /** 特定のブコメに対するスター情報を再読み込み */
        suspend fun update(bookmark: Bookmark, onError: OnError? = null) {
            try {
                val result = client.getStarsEntryAsync(bookmark.getBookmarkUrl(entry)).await()
                repository.setStarsEntryTo(bookmark.user, result)

                val old = value ?: emptyList()
                val new = old.filterNot { it.url == result.url }.plus(result)

                postValue(new)
            }
            catch (e: Throwable) {
                onError?.invoke(e)
            }
        }

        /** スター情報を再読み込み */
        fun updateAsync(forceUpdate: Boolean = false) : Deferred<List<StarsEntry>> {
            task?.cancel()

            val userAndUrls =
                repository.bookmarksEntry?.bookmarks
                    ?.filter {
                        (forceUpdate || repository.getStarsEntryTo(it.user) == null) && it.comment.isNotBlank()
                    }
                    ?.map { Pair(it.user, it.getBookmarkUrl(entry)) }
                    ?: emptyList()

            return client.getStarsEntryAsync(userAndUrls.map { it.second }).also {
                it.invokeOnCompletion { e ->
                    if (e == null) {
                        val result = it.getCompleted()

                        result.forEach { starEntry ->
                            val user = userAndUrls.firstOrNull { pair ->
                                pair.second == starEntry.url
                            }?.first

                            if (user != null) {
                                repository.setStarsEntryTo(user, starEntry)
                            }
                        }

                        // タスク完了
                        task = null

                        postValue(result)
                    }
                }

                task = it
            }
        }
    }

// ------ //

    /** ブクマに付けられたスター情報取得を監視するライブデータ */
    class StarsEntryLiveData(
        private val client: HatenaClient,
        private val entry: Entry,
        private val bookmark: Bookmark,
        private val repository: BookmarksRepository
    ) : LiveData<StarsEntry?>() {
        private var task: Deferred<StarsEntry>? = null

        override fun onActive() {
            updateAsync().start()
        }

        override fun onInactive() {
            task?.cancel()
            task = null
        }

        /** 再通知 */
        fun notifyReload() {
            postValue(value)
        }

        /** スター情報を強制再読み込み */
        fun updateAsync(forceUpdate: Boolean = false) : Deferred<StarsEntry> {
            task?.cancel()
            task = null

            val cache = repository.getStarsEntryTo(bookmark.user)
            return if (!forceUpdate && cache != null) {
                GlobalScope.async {
                    return@async cache.also {
                        postValue(it)
                    }
                }
            }
            else {
                client.getStarsEntryAsync(bookmark.getBookmarkUrl(entry)).also {
                    it.invokeOnCompletion { e ->
                        if (e == null) {
                            val result = it.getCompleted()
                            repository.setStarsEntryTo(bookmark.user, result)
                            postValue(result)
                        }
                    }
                    task = it
                }
            }
        }
    }

}
