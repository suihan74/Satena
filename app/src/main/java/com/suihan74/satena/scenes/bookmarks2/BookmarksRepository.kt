package com.suihan74.satena.scenes.bookmarks2

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.suihan74.hatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.exceptions.InvalidUrlException
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
        get() = synchronized(entryLock) { _entry!! }
        private set(value) {
            synchronized(entryLock) {
                _entry = value
            }
        }

    private var _entry: Entry? = null

    /** ブクマエントリ */
    var bookmarksEntry: BookmarksEntry? = null
        get() = synchronized(bookmarksEntryLock) { field }
        private set(value) {
            synchronized(bookmarksEntryLock) {
                field = value
            }
        }

    /** 人気ブクマリストを含む概要データのキャッシュ */
    var bookmarksDigest: BookmarksDigest? = null
        get() = synchronized(bookmarksDigestLock) { field }
        private set(value) {
            synchronized(bookmarksDigestLock) {
                field = value
            }
        }

    /** 人気ブクマリストのキャッシュ */
    var bookmarksPopular: List<Bookmark> = emptyList()
        get() = synchronized(bookmarksPopularLock) { field }
        private set(value) {
            synchronized(bookmarksPopularLock) {
                field = value
            }
        }

    /** 新着ブクマリストのキャッシュ */
    var bookmarksRecent: List<Bookmark> = emptyList()
        get() = synchronized(bookmarksRecentLock) { field }
        private set(value) {
            synchronized(bookmarksRecentLock) {
                field = value
            }
        }

    /** 非表示ユーザーIDリストのキャッシュ */
    var ignoredUsers: List<String> = emptyList()
        get() = synchronized(ignoredUsersLock) { field }
        private set(value) {
            synchronized(ignoredUsersLock) {
                field = value
            }
        }

    /** スター情報のキャッシュ -> key = 対象ユーザー名, value = スター情報 */
    private val starsMap = HashMap<String, StarsEntry>()

    /** ログイン状態 */
    val signedIn
        get() = client.signedIn()

    /** ログイン中のユーザー名 */
    val userSignedIn
        get() = client.account?.name

    /** ドロワ位置 */
    val drawerGravity : Int by lazy {
        prefs.getInt(PreferenceKey.DRAWER_GRAVITY)
    }

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
        StarsLiveData(this)
    }

    /** スター送信前に確認する */
    val usePostStarDialog by lazy {
        prefs.getBoolean(PreferenceKey.USING_POST_STAR_DIALOG)
    }

    /** IDコールされた非表示ユーザーを表示する */
    val showCalledIgnoredUsers by lazy {
        prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING)
    }

    /** リンクをシングルタップしたときの処理 */
    val linkSingleTapAction : TapEntryAction by lazy {
        TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION))
    }

    /** リンクをロングタップしたときの処理 */
    val linkLongTapAction : TapEntryAction by lazy {
        TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION))
    }

    private val entryLock by lazy { Any() }
    private val bookmarksEntryLock by lazy { Any() }
    private val bookmarksDigestLock by lazy { Any() }
    private val bookmarksPopularLock by lazy { Any() }
    private val bookmarksRecentLock by lazy { Any() }
    private val ignoredUsersLock by lazy { Any() }
    private val recentBookmarksCursorLock by lazy { Any() }

    // ------ //

    /** リポジトリ初期化 */
    suspend fun init() {
        accountLoader.signInAccounts()
    }

    /** ユーザーに付けられたスター情報を取得する */
    fun getStarsEntryTo(user: String) = synchronized(starsMap) { starsMap[user] }
    /** ユーザーに付けられたスター情報を登録or更新する */
    private fun setStarsEntryTo(user: String, entry: StarsEntry) = synchronized(starsMap) { starsMap[user] = entry }

    /** ユーザーが付けたスター情報を取得する */
    fun getStarsEntryFrom(user: String) = synchronized(starsMap) {
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
                    val result = kotlin.runCatching {
                        client.getRecentBookmarksAsync(url = url, cursor = cursor).await()
                    }

                    val response = result.getOrNull() ?: break

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
        additionalLoading: Boolean = false,
        leastCommentsNum: Int = 10
    ) : Deferred<List<Bookmark>> = client.async(Dispatchers.Default) {
        if (additionalLoading && recentBookmarksCursor == null) return@async emptyList()

        val response =
            if (!additionalLoading) loadMostRecentBookmarksAsync(url = entry.url).await()
            else client.getRecentBookmarksAsync(
                    url = entry.url,
                    cursor = recentBookmarksCursor
                ).await()

        var cursor = response.cursor
        val page = ArrayList<Bookmark>(
            response.bookmarks.map { Bookmark.create(it) }
        )

        // 有言ブクマを一定数確保するために繰り返し続きをロードする
        while (cursor != null && page.count { it.comment.isNotBlank() } < leastCommentsNum) {
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
            allStarsLiveData.update()
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
    suspend fun reportBookmark(report: Report) : Boolean {
        val result = runCatching {
            client.reportAsync(report).await()
        }
        return result.isSuccess
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
        private val repository: BookmarksRepository
    ) : LiveData<List<StarsEntry>>(emptyList()) {
        private var task: Deferred<List<StarsEntry>>? = null
            get() = synchronized(this) { field }
            private set(value) {
                synchronized(this) { field = value }
            }

        private val client : HatenaClient
            get() = repository.client

        /** ロード中か否か */
        val loading: Boolean
            get() = task != null

        /** 再通知 */
        fun notifyReload() {
            postValue(value)
        }

        override fun onActive() {
            client.launch {
                kotlin.runCatching {
                    update()
                }
            }
        }

        override fun onInactive() {
            task?.cancel()
            task = null
        }

        /** 特定のブコメに対するスター情報を再読み込み */
        suspend fun update(
            bookmark: Bookmark
        ) = withContext(Dispatchers.Default) {
            try {
                val entry = repository.entry

                val client = repository.client
                val result = client.getStarsEntryAsync(bookmark.getBookmarkUrl(entry)).await()
                repository.setStarsEntryTo(bookmark.user, result)

                val old = value ?: emptyList()
                val new = old.filterNot { it.url == result.url }.plus(result)

                postValue(new)
            }
            catch (e: Throwable) {
                throw e
            }
        }

        /** スター情報を再読み込み */
        suspend fun update(
            forceUpdate: Boolean = false
        ) : List<StarsEntry> = withContext(Dispatchers.Default) {
            task?.cancel()

            val entry = repository.entry

            val userAndUrls =
                repository.bookmarksEntry?.bookmarks
                    ?.filter {
                        (forceUpdate || repository.getStarsEntryTo(it.user) == null) && it.comment.isNotBlank()
                    }
                    ?.map { Pair(it.user, it.getBookmarkUrl(entry)) }
                    ?: emptyList()

            val task = client.getStarsEntryAsync(userAndUrls.map { it.second })
            this@StarsLiveData.task = task

            return@withContext try {
                val result = task.await()

                result.forEach { starEntry ->
                    val user = userAndUrls.firstOrNull { pair ->
                        pair.second == starEntry.url
                    }?.first

                    if (user != null) {
                        repository.setStarsEntryTo(user, starEntry)
                    }
                }

                // タスク完了
                this@StarsLiveData.task = null
                postValue(result)

                result
            }
            catch (e: Throwable) {
                throw e
            }
        }
    }

// ------ //

    /** ブクマに付けられたスター情報取得を監視するライブデータ */
    class StarsEntryLiveData(
        private val bookmark: Bookmark,
        private val repository: BookmarksRepository
    ) : LiveData<StarsEntry?>() {
        private var task: Deferred<StarsEntry>? = null

        private val client: HatenaClient
            get() = repository.client

        override fun onActive() {
            client.launch {
                kotlin.runCatching {
                    update()
                }
            }
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
        suspend fun update(
            forceUpdate: Boolean = false
        ) : StarsEntry = withContext(Dispatchers.Default) {
            task?.cancel()
            task = null

            val cache = repository.getStarsEntryTo(bookmark.user)

            if (!forceUpdate && cache != null) {
                return@withContext cache.also { postValue(it) }
            }

            return@withContext try {
                val entry = repository.entry
                val task = client.getStarsEntryAsync(bookmark.getBookmarkUrl(entry))
                this@StarsEntryLiveData.task = task
                val result = task.await()
                repository.setStarsEntryTo(bookmark.user, result)
                postValue(result)

                result
            }
            catch (e: Throwable) {
                throw e
            }
        }
    }

}
