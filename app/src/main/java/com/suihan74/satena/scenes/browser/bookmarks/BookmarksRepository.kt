package com.suihan74.satena.scenes.browser.bookmarks

import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import com.suihan74.satena.models.userTag.UserTagDao
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepositoryForBookmarks
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersRepositoryInterface
import com.suihan74.utilities.*
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.suihan74.utilities.extensions.updateFirstOrPlus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * ブクマ画面用のリポジトリ
 *
 * TODO: v1.6で.bookmarkと統合することを前提に開発すること
 */
class BookmarksRepository(
    private val accountLoader: AccountLoader,
    private val prefs : SafeSharedPreferences<PreferenceKey>,
    private val ignoredEntryDao : IgnoredEntryDao,
    private val userTagDao: UserTagDao
) :
        // ユーザー非表示
        IgnoredUsersRepositoryInterface by IgnoredUsersRepository(accountLoader),
        // NGワード
        IgnoredEntriesRepositoryForBookmarks by IgnoredEntriesRepository(ignoredEntryDao),
        // ユーザータグ
        UserTagsRepositoryInterface by UserTagsRepository(userTagDao),
        // スター
        StarRepositoryInterface by StarRepository(accountLoader, prefs)
{

    /** はてなアクセス用クライアント */
    private val client = accountLoader.client

    /** サインイン状態 */
    val signedIn by lazy {
        SingleUpdateMutableLiveData(client.signedIn())
    }

    /** サインインしているユーザー名 */
    val userSignedIn : String?
        get() = client.account?.name

    /** アカウントが必要な操作前にサインインする */
    suspend fun signIn() : Account? = withContext(Dispatchers.Default) {
        val result = runCatching {
            accountLoader.signInAccounts(reSignIn = false)
        }

        val account =
            if (result.isSuccess) client.account
            else null

        val signedIn = account != null

        this@BookmarksRepository.signedIn.postValue(signedIn)

        if (signedIn) {
            loadUserColorStarsCount()
        }

        return@withContext account
    }

    // ------ //

    // エントリ情報
    var url: String = ""
        private set

    /** エントリ情報 */
    val entry by lazy {
        MutableLiveData<Entry?>()
    }

    /** エントリをロード中かどうか */
    val loadingEntry by lazy {
        SingleUpdateMutableLiveData<Boolean>(false)
    }

    /** ブクマを含むエントリ情報 */
    val bookmarksEntry by lazy {
        MutableLiveData<BookmarksEntry?>()
    }

    // 各タブでの表示用のブクマリスト

    private val lazyBookmarksList
        get() = lazy {
            MutableLiveData<List<Bookmark>>()
        }

    /** 人気ブクマ */
    val popularBookmarks by lazyBookmarksList

    /** 新着ブクマ */
    val recentBookmarks by lazyBookmarksList

    /** 全ブクマ */
    val allBookmarks by lazyBookmarksList

    /** カスタム */
    val customBookmarks by lazyBookmarksList

    // 取得したブクマデータのキャッシュ

    /** 人気ブクマ、関連記事、お気に入りユーザーのブクマ */
    private var bookmarksDigestCache : BookmarksDigest? = null

    /** 非表示対象、無言を含むすべての新着ブクマ */
    private var bookmarksRecentCache : List<BookmarkWithStarCount> = emptyList()

    /** 新着ブクマの追加取得用カーソル */
    private var recentCursor : String? = null

    /** 「すべて」ブクマリストでは非表示対象を表示する */
    private val showIgnoredUsersInAllBookmarks by lazy {
        prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS)
    }

    // ------ //

    /** ブクマ投稿後に呼ばれるイベント */
    var afterPosted : Listener<BookmarkResult>? = null
        private set

    /** ブクマ投稿後に呼ばれるイベントをセットする */
    fun setAfterPostedListener(l: Listener<BookmarkResult>?) {
        afterPosted = l
    }

    // ------ //

    /**
     * URLを渡して必要な初期化を行う
     */
    suspend fun launchLoadingUrl(
        url: String,
        onFinally: OnFinally?
    ) = withContext(Dispatchers.Default) {
        loadingEntry.postValue(true)

        val modifyResult = runCatching {
            modifySpecificUrls(url)
        }
        val modifiedUrl = modifyResult.getOrNull() ?: url
        this@BookmarksRepository.url = modifiedUrl

        bookmarksDigestCache = null
        bookmarksRecentCache = emptyList()

        try {
            val loadingIgnoresTasks = listOf(
                async { loadIgnoredWordsForBookmarks() },
                async { loadIgnoredUsers() }
            )
            loadingIgnoresTasks.awaitAll()
            loadEntry(modifiedUrl)

            loadingEntry.postValue(false)

            val loadingContentsTasks = listOf(
                async {
                    runCatching {
                        loadBookmarksEntry(modifiedUrl)
                    }
                },
                async {
                    runCatching {
                        loadPopularBookmarks()
                    }
                },
                async {
                    runCatching {
                        loadRecentBookmarks(additionalLoading = false)
                    }
                }
            )
            loadingContentsTasks.awaitAll()
        }
        catch (e: Throwable) {
            loadingEntry.postValue(false)
            Log.e("BookmarksRepo", Log.getStackTraceString(e))
        }
        finally {
            withContext(Dispatchers.Main) {
                onFinally?.invoke()
            }
        }
    }

    /** 取得済みのエントリ情報をセットする */
    @MainThread
    fun loadEntry(e: Entry) {
        entry.value = e
    }

    /** エントリ情報をロードする */
    @Throws(ConnectionFailureException::class)
    suspend fun loadEntry(url: String) = withContext(Dispatchers.Main) {
        val result = runCatching {
            client.getEntryAsync(url).await()
        }

        entry.value = result.getOrElse {
            throw ConnectionFailureException(it)
        }
    }

    /** エントリ情報をロードする */
    @Throws(ConnectionFailureException::class)
    suspend fun loadEntry(eid: Long) = withContext(Dispatchers.Main) {
        val result = runCatching {
            client.getEntryAsync(eid).await()
        }

        entry.value = result.getOrElse {
            throw ConnectionFailureException(it)
        }
    }

    /** ブクマエントリ情報をロードする */
    @Throws(ConnectionFailureException::class)
    suspend fun loadBookmarksEntry(url: String) = withContext(Dispatchers.Default) {
        val result = runCatching {
            client.getBookmarksEntryAsync(url).await()
        }

        val e = result.getOrElse {
            throw ConnectionFailureException(it)
        }

        bookmarksEntry.postValue(e)
    }

    // ------ //

    /** ブクマが非表示対象かを判別する */
    fun checkIgnored(bookmark: Bookmark) : Boolean {
        if (ignoredUsersCache.any { bookmark.user == it }) return true
        return ignoredWordsForBookmarks.any { w ->
            bookmark.commentRaw.contains(w)
                    || bookmark.user.contains(w)
                    || bookmark.tags.any { t -> t.contains(w) }
        }
    }

    /** ブクマが非表示対象かを判別する */
    fun checkIgnored(bookmark: BookmarkWithStarCount) : Boolean {
        if (ignoredUsersCache.any { bookmark.user == it }) return true
        return ignoredWordsForBookmarks.any { w ->
            bookmark.comment.contains(w)
                    || bookmark.user.contains(w)
                    || bookmark.tags.any { t -> t.contains(w) }
        }
    }

    /** 非表示対象を除外する */
    @WorkerThread
    private fun filterIgnored(src: List<BookmarkWithStarCount>) : List<Bookmark> {
        return src
            .filterNot { b -> checkIgnored(b) }
            .map { Bookmark.create(it) }
    }

    /** 各種リストに変更を通知する */
    @WorkerThread
    private fun updateRecentBookmarksLiveData(rawList: List<BookmarkWithStarCount>) {

        val items = filterIgnored(rawList.filter { b ->
            b.comment.isNotBlank()
        })
        recentBookmarks.postValue(items)

        allBookmarks.postValue(
            if (showIgnoredUsersInAllBookmarks) {
                rawList.map { Bookmark.create(it) }
            }
            else filterIgnored(rawList)
        )
        // TODO: カスタムタブ
        customBookmarks.postValue(
            emptyList()
        )
    }

    /** 読み込み済みの各種リストを再生成する */
    suspend fun refreshBookmarks() = withContext(Dispatchers.Default) {
        // 人気ブクマリストを再生成
        popularBookmarks.postValue(
            filterIgnored(bookmarksDigestCache?.scoredBookmarks ?: emptyList())
        )

        // 新着ブクマリストを再生成
        updateRecentBookmarksLiveData(bookmarksRecentCache)
    }

    /** 他の画面から復帰時にキャッシュを再読み込みする */
    suspend fun onRestart() {
        loadIgnoredUsers()
        loadUserTags()

        val users = bookmarksDigestCache?.scoredBookmarks?.map { it.user }
            ?.plus(
                bookmarksRecentCache.map { it.user }
            )
            ?.distinct() ?: emptyList()

        users.forEach { user ->
            loadUserTags(user, forceRefresh = true)
        }

        refreshBookmarks()
    }

    // ------ //

    /** 同じユーザーのブクマを渡された内容に更新する */
    suspend fun updateBookmark(
        result: BookmarkResult
    ) = withContext(Dispatchers.Default) {
        entry.postValue(
            entry.value?.copy(
                id = result.eid ?: entry.value?.id ?: 0L,
                bookmarkedData = result
            )
        )
        val bookmark = BookmarkWithStarCount(
            BookmarkWithStarCount.User(result.user, result.userIconUrl),
            comment = result.comment,
            isPrivate = result.private ?: false,
            link = result.permalink,
            tags = result.tags,
            timestamp = result.timestamp,
            starCount = result.starsCount?.map {
                StarCount(it.color, it.count)
            } ?: emptyList()
        )
        updateBookmark(bookmark)
    }

    /** 同じユーザーのブクマを渡された内容に更新する */
    suspend fun updateBookmark(
        bookmark: BookmarkWithStarCount
    ) = withContext(Dispatchers.Default) {
        val user = bookmark.user

        val bEntry = bookmarksEntry.value?.let { e ->
            val b = Bookmark.create(bookmark)
            e.copy(
                bookmarks = e.bookmarks.updateFirstOrPlus(b) { it.user == user }
            )
        }
        bookmarksEntry.postValue(bEntry)

        bookmarksDigestCache = BookmarksDigest(
            bookmarksDigestCache?.referredBlogEntries,
            bookmarksDigestCache?.scoredBookmarks?.map {
                if (it.user == user) bookmark else it
            } ?: emptyList(),
            bookmarksDigestCache?.favoriteBookmarks?.map {
                if (it.user == user) bookmark else it
            } ?: emptyList()
        )

        bookmarksRecentCache = bookmarksRecentCache.updateFirstOrPlus(bookmark) { it.user == user }

        refreshBookmarks()
    }

    /** 人気ブクマリストを取得する */
    @Throws(
        InvalidUrlException::class,
        ConnectionFailureException::class,
        TimeoutException::class,
        NotFoundException::class
    )
    suspend fun loadPopularBookmarks() = withContext(Dispatchers.Default) {
        val result = runCatching {
            client.getDigestBookmarksAsync(url).await()
        }

        val digest = result.getOrElse {
            when (it) {
                is TimeoutException, is NotFoundException ->
                    throw it

                else ->
                    throw ConnectionFailureException(it)
            }
        }

        bookmarksDigestCache = digest
        val bookmarks = filterIgnored(digest.scoredBookmarks)
        bookmarks.forEach {
            loadUserTags(it.user)
        }

        popularBookmarks.postValue(bookmarks)
    }

    /** 新着ブクマリストを取得する */
    @Throws(
        InvalidUrlException::class,
        TimeoutException::class,
        NotFoundException::class,
        ConnectionFailureException::class
    )
    suspend fun loadRecentBookmarks(
        additionalLoading: Boolean = false
    ) = withContext(Dispatchers.Default) {
        // 既に最後までロードしている
        if (additionalLoading && recentCursor == null) {
            return@withContext
        }

        val result = kotlin.runCatching {
            if (additionalLoading) {
                client.getRecentBookmarksAsync(
                    url = url,
                    cursor = recentCursor
                ).await()
            }
            else {
                loadMostRecentBookmarks(url)
            }
        }

        if (result.isFailure) {
            throw result.exceptionOrNull() ?: ConnectionFailureException()
        }

        val response = result.getOrNull()!!

        if (additionalLoading || bookmarksRecentCache.isEmpty()) {
            recentCursor = response.cursor
        }
        // 重複がある場合は新しい方に更新する
        bookmarksRecentCache = bookmarksRecentCache
            .filterNot { existed ->
                response.bookmarks.any { b ->
                    existed.user == b.user
                }
            }
            .plus(response.bookmarks)
            .sortedByDescending { it.timestamp }

        // ユーザータグを更新
        response.bookmarks.forEach {
            loadUserTags(it.user)
        }

        // 各表示用リストに変更を通知する
        updateRecentBookmarksLiveData(bookmarksRecentCache)
    }

    /** 最新のブクマリストを(取得済みの位置まで)取得する */
    @Throws(
        TimeoutException::class,
        NotFoundException::class
    )
    private suspend fun loadMostRecentBookmarks(url: String) : BookmarksWithCursor {
        val bookmarks = ArrayList<BookmarkWithStarCount>()
        var cursor: String? = null
        val threshold = bookmarksRecentCache.firstOrNull()?.timestamp

        do {
            val result = runCatching {
                client.getRecentBookmarksAsync(
                    url = url,
                    cursor = cursor
                ).await()
            }

            when (val e = result.exceptionOrNull()) {
                is TimeoutException, is NotFoundException ->
                    throw e
            }

            val shouldBreak =
                result.getOrNull()?.let { response ->
                    bookmarks.addAll(response.bookmarks)
                    cursor = response.cursor

                    cursor != null
                    || threshold == null
                    || response.bookmarks.lastOrNull {
                        it.timestamp <= threshold
                    } != null
                } ?: true

        } while (!shouldBreak)

        return BookmarksWithCursor(
            bookmarks = bookmarks,
            cursor = cursor
        )
    }
}
