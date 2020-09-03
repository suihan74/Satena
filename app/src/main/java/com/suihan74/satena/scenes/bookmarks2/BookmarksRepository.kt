package com.suihan74.satena.scenes.bookmarks2

import androidx.lifecycle.LiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.suihan74.utilities.lock
import kotlinx.coroutines.*

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksRepository(
    private val client: HatenaClient,
    private val accountLoader: AccountLoader
) {
    /** エントリ情報が正しく設定されているか */
    val isInitialized : Boolean get() =
        this::entry.isInitialized

    /** エントリ情報 */
    lateinit var entry: Entry
        private set

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
        private set

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

    /** 新着ブクマを取得 */
    fun loadBookmarksRecentAsync(offset: Long? = null) =
        client.getRecentBookmarksAsync(entry.url, of = offset).apply {
            invokeOnCompletion { e ->
                if (e != null) return@invokeOnCompletion

                val page = getCompleted().map { Bookmark.create(it) }
                bookmarksRecent = page
                    .plus(
                        bookmarksRecent.filterNot { page.any { updated -> it.user == updated.user } }
                    )
                    .sortedByDescending { it.timestamp }

                // エントリ情報のブクマリストにも追加する
                val bEntry = bookmarksEntry
                if (bEntry != null) {
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
            }
        }

    /** 新着ブクマリストの次のページをロードする */
    fun loadNextBookmarksRecentAsync() =
        loadBookmarksRecentAsync(bookmarksRecent.size.toLong())

    /** 非表示ユーザーのリストをロードする */
    fun loadIgnoredUsersAsync(
        forceUpdate: Boolean = false
    ) : Deferred<List<String>> = client.async(Dispatchers.Default) {
        try {
            val old = ignoredUsers
            val new = client.getIgnoredUsersAsync().await()
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
    suspend fun postStar(bookmark: Bookmark, color: StarColor, quote: String = "") {
        client.postStarAsync(bookmark.getBookmarkUrl(entry), color, quote).await()
        userStarsLiveData.load()
    }

    /** ブクマに付けられたスター取得を監視するライブデータを生成する */
    fun createStarsEntryLiveData(bookmark: Bookmark) =
        StarsEntryLiveData(
            client,
            entry,
            bookmark,
            this)

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
    ) : LiveData<List<StarsEntry>>() {
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
