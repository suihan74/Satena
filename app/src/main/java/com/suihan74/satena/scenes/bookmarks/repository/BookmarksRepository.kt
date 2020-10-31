package com.suihan74.satena.scenes.bookmarks.repository

import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
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
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.OnFinally
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SingleUpdateMutableLiveData
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.updateFirstOrPlusAhead
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
    val accountLoader: AccountLoader,
    val prefs : SafeSharedPreferences<PreferenceKey>,
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
    companion object {
        /** ブコメ最大文字数 */
        const val MAX_COMMENT_LENGTH = 100

        /** 同時使用可能な最大タグ数 */
        const val MAX_TAGS_COUNT = 10
    }


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

    /**
     * 与えられた文字列を含むブクマを抽出する
     *
     * 空白区切りで複数設定、部分一致
     */
    val filteringText by lazy {
        MutableLiveData<String?>() // null or blank で無効化
    }

    // ------ //

    /**
     * URLを渡して必要な初期化を行う
     */
    suspend fun loadBookmarks(
        url: String,
        onFinally: OnFinally? = null
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
            Log.e("BookmarksRepo", Log.getStackTraceString(e))
        }
        finally {
            withContext(Dispatchers.Main) {
                loadingEntry.value = false
                onFinally?.invoke()
            }
        }
    }

    /** 取得済みのエントリ情報をセットする */
    @MainThread
    fun loadEntry(e: Entry) {
        entry.value = e
    }

    /**
     * エントリ情報をロードする
     *
     * @throws ConnectionFailureException
     */
    suspend fun loadEntry(url: String){
        val result = runCatching {
            client.getEntryAsync(url).await()
        }

        withContext(Dispatchers.Main) {
            entry.value = result.getOrElse {
                throw ConnectionFailureException(it)
            }
        }
    }

    /**
     * エントリ情報をロードする
     *
     * @throws ConnectionFailureException
     */
    suspend fun loadEntry(eid: Long) {
        val result = runCatching {
            client.getEntryAsync(eid).await()
        }

        withContext(Dispatchers.Main) {
            entry.value = result.getOrElse {
                throw ConnectionFailureException(it)
            }
        }
    }

    /**
     * ブクマエントリ情報をロードする
     *
     * @throws ConnectionFailureException
     */
    suspend fun loadBookmarksEntry(url: String) {
        val result = runCatching {
            client.getBookmarksEntryAsync(url).await()
        }

        val e = result.getOrElse {
            throw ConnectionFailureException(it)
        }

        withContext(Dispatchers.Main) {
            bookmarksEntry.value = e
        }
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
    private fun filterIgnored(src: List<BookmarkWithStarCount>) : List<Bookmark> {
        return src
            .filterNot { b -> checkIgnored(b) }
            .map { Bookmark.create(it) }
    }

    /** キーワードで抽出する */
    private fun wordFilter(src: List<BookmarkWithStarCount>) : List<BookmarkWithStarCount> {
        val keywordsRaw = filteringText.value
        if (keywordsRaw.isNullOrBlank()) return src

        val keywords = keywordsRaw.split(Regex("""\s+"""))
        val keywordsRegex = Regex(keywords.joinToString(separator = "|") { Regex.escape(it) })

        return src.filter { b ->
            keywordsRegex.containsMatchIn(b.string)
        }
    }

    /** 新着ブクマリストに依存する各種リストに変更を通知する */
    @WorkerThread
    private fun updateRecentBookmarksLiveData(rawList: List<BookmarkWithStarCount>) {
        val list = wordFilter(rawList)

        // 新着
        val items = filterIgnored(list.filter { b ->
            b.comment.isNotBlank()
        })
        recentBookmarks.postValue(items)

        // すべて
        allBookmarks.postValue(
            if (showIgnoredUsersInAllBookmarks) list.map { Bookmark.create(it) }
            else filterIgnored(list)
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
            filterIgnored(
                wordFilter(
                    bookmarksDigestCache?.scoredBookmarks.orEmpty()
                )
            )
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
                bookmarks = e.bookmarks.updateFirstOrPlusAhead(b) { it.user == user }
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

        bookmarksRecentCache =
            bookmarksRecentCache.updateFirstOrPlusAhead(bookmark) { it.user == user }

        refreshBookmarks()
    }

    /**
     * 人気ブクマリストを取得する
     *
     * @throws ConnectionFailureException
     * @throws TimeoutException
     * @throws NotFoundException
     */
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

        // スター情報を取得する
        loadStarsEntriesForBookmarks(bookmarks)
    }

    /**
     *  新着ブクマリストを取得する
     *
     * @throws ConnectionFailureException
     * @throws TimeoutException
     * @throws NotFoundException
     */
    suspend fun loadRecentBookmarks(
        additionalLoading: Boolean = false
    ) = withContext(Dispatchers.Default) {
        // 既に最後までロードしている
        if (additionalLoading && recentCursor == null) {
            return@withContext
        }

        val result = runCatching {
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

        // スター情報を取得する
        loadStarsEntriesForBookmarksWithStarCount(response.bookmarks)
    }

    /**
     * 最新のブクマリストを(取得済みの位置まで)取得する
     *
     * @throws TimeoutException
     * @throws NotFoundException
     */
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

    // ------ //

    /**
     * 対象ブクマにつけられたスター情報を取得する
     *
     * @return 取得失敗時null
     */
    suspend fun getStarsEntry(bookmark: Bookmark, forceUpdate: Boolean = false) : LiveData<StarsEntry>? {
        return entry.value?.let { entry ->
            getStarsEntry(bookmark.getBookmarkUrl(entry), forceUpdate)
        }
    }

    /**
     * 渡された全ブクマにつけられたスター情報を取得し、キャッシュしておく
     */
    suspend fun loadStarsEntriesForBookmarksWithStarCount(bookmarks: List<BookmarkWithStarCount>) {
        entry.value?.let { entry ->
            val bookmarkUrls = bookmarks.filter {
                it.starCount.any { s -> s.count > 0 }
            }.map {
                it.getBookmarkUrl(entry)
            }
            loadStarsEntries(bookmarkUrls)
        }
    }

    /**
     * 渡された全ブクマにつけられたスター情報を取得し、キャッシュしておく
     */
    suspend fun loadStarsEntriesForBookmarks(bookmarks: List<Bookmark>) {
        entry.value?.let { entry ->
            val bookmarkUrls = bookmarks.filter {
                it.comment.isNotBlank()
            }.map {
                it.getBookmarkUrl(entry)
            }
            loadStarsEntries(bookmarkUrls)
        }
    }

    /**
     * 自分が対象ブクマにスターをつけているか確認する
     */
    suspend fun getUserStars(bookmark: Bookmark, user: String) : List<Star>? {
        val starsEntry = getStarsEntry(bookmark, forceUpdate = false)?.value
        return starsEntry?.allStars?.filter { it.user == user }
    }

    // ------ //

    /**
     * ブクマを削除する
     *
     * @throws TaskFailureException
     */
    suspend fun deleteBookmark(bookmark: Bookmark) = withContext(Dispatchers.Default) {
        val url = entry.value?.url ?: throw TaskFailureException("invalid entry")
        val user = bookmark.user

        if (user != userSignedIn) {
            throw TaskFailureException("it's not the signed-in user's bookmark")
        }

        val result = runCatching {
            client.deleteBookmarkAsync(url).await()
        }

        if (result.isFailure) {
            throw TaskFailureException(cause = result.exceptionOrNull())
        }

        // 表示を更新する

        entry.postValue(
            entry.value?.copy(bookmarkedData = null)
        )

        val bEntry = bookmarksEntry.value?.let { e ->
            e.copy(
                bookmarks = e.bookmarks.filterNot { it.user == user }
            )
        }
        bookmarksEntry.postValue(bEntry)

        bookmarksDigestCache = BookmarksDigest(
            bookmarksDigestCache?.referredBlogEntries,
            bookmarksDigestCache?.scoredBookmarks?.filterNot { it.user == user }.orEmpty(),
            bookmarksDigestCache?.favoriteBookmarks?.filterNot { it.user == user }.orEmpty()
        )

        bookmarksRecentCache =
            bookmarksRecentCache.filterNot { it.user == user }

        refreshBookmarks()
    }
}