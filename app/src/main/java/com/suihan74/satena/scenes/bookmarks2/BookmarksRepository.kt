package com.suihan74.satena.scenes.bookmarks2

import androidx.lifecycle.LiveData
import com.suihan74.HatenaLib.*
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.lock
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class BookmarksRepository(
    private val client: HatenaClient,
    private val accountLoader: AccountLoader
) {
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

    /** 所持スター情報のキャッシュ */
    var userStars: UserColorStarsCount? = null
        private set

    /** ログイン状態 */
    val signedIn
        get() = client.signedIn()

    /** ログイン中のユーザー名 */
    val userSignedIn
        get() = client.account?.name

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
        val existed = client.searchEntriesAsync(url, SearchType.Text).await()
            .firstOrNull { it.url == url }
        entry = existed ?: client.getEmptyEntryAsync(url).await()
    }

    /** 既にロード済みのエントリ情報をリポジトリにセットする */
    fun setEntry(entry: Entry) {
        this.entry = entry
    }

    /** ブックマークエントリを取得 */
    fun loadBookmarksEntryAsnyc() =
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
                    bookmarksPopular = result.scoredBookmarks.map { Bookmark.createFrom(it) }
                }
            }
        }

    /** 新着ブクマを取得 */
    fun loadBookmarksRecentAsync(offset: Long? = null) =
        client.getRecentBookmarksAsync(entry.url, of = offset).apply {
            invokeOnCompletion { e ->
                if (e != null) return@invokeOnCompletion

                val page = getCompleted().map { Bookmark.createFrom(it) }
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

    /** 表示ユーザーリストを監視するライブデータを生成する */
    val ignoredUsersLiveData by lazy {
        IgnoredUsersLiveData(this)
    }

    /** 非表示ユーザーのリストをロードする */
    fun loadIgnoredUsersAsync() =
        client.getIgnoredUsersAsync().apply {
            invokeOnCompletion { e ->
                if (e == null) {
                    ignoredUsers = getCompleted()
                    ignoredUsersLiveData.notifyPost()
                }
            }
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
    fun unignoreUserAsync(user: String) =
        client.unignoreUserAsync(user).apply {
            invokeOnCompletion { e ->
                if (e == null) {
                    ignoredUsers = ignoredUsers.minus(user)
                    ignoredUsersLiveData.notifyPost()
                }
            }
        }

    /** サインインしているユーザーの所持スター情報を監視するライブデータを作成する */
    val userStarsLiveData by lazy {
        UserStarsLiveData(client, this)
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

    /** エントリ中のブクマについたすべてのスター取得を監視するライブデータを作成する */
    val allStarsLiveData by lazy {
        StarsLiveData(
            client,
            entry,
            this
        )
    }

    /** ブクマを通報 */
    suspend fun reportBookmark(report: Report) {
        client.reportAsync(report).await()
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
        private val client: HatenaClient,
        private val repository: BookmarksRepository
    ) : LiveData<UserColorStarsCount>() {
        override fun getValue() = repository.userStars

        suspend fun load() =
            if (client.signedIn()) {
                val result = client.getMyColorStarsAsync().await()
                repository.userStars = result
                postValue(result)
            }
            else {
                val dummy = UserColorStarsCount(0, 0, 0, 0)
                repository.userStars = dummy
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
    ) : LiveData<StarsEntry>() {
        private var task: Deferred<StarsEntry>? = null

        override fun onActive() {
            updateAsync().start()
        }

        override fun onInactive() {
            task?.cancel()
        }

        /** スター情報を強制再読み込み */
        fun updateAsync(forceUpdate: Boolean = false) : Deferred<StarsEntry> {
            this.task?.cancel()

            val cache = repository.getStarsEntryTo(bookmark.user)
            return if (!forceUpdate && cache != null) {
                GlobalScope.async {
                    return@async cache!!.also {
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
                    this.task = it
                }
            }
        }
    }

}
