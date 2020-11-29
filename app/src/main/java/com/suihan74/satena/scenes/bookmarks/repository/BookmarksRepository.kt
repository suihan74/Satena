package com.suihan74.satena.scenes.bookmarks.repository

import android.content.Intent
import android.util.Log
import android.webkit.URLUtil
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import com.suihan74.satena.models.userTag.UserTagDao
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepositoryForBookmarks
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersRepositoryInterface
import com.suihan74.utilities.*
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.getObjectExtra
import com.suihan74.utilities.extensions.updateFirstOrPlusAhead
import kotlinx.coroutines.*

/**
 * ブクマ画面用のリポジトリ
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
        // Intentからエントリ情報を引き出すためのキー

        /** Entryを直接渡す場合 */
        const val EXTRA_ENTRY = "BookmarksRepository.EXTRA_ENTRY"

        /** EntryのURLを渡す場合 */
        const val EXTRA_ENTRY_URL = "BookmarksRepository.EXTRA_ENTRY_URL"

        /** EntryのIDを渡す場合 */
        const val EXTRA_ENTRY_ID = "BookmarksRepository.EXTRA_ENTRY_ID"

        // 内部的な設定

        /** 一度の新着ブクマ取得ごとのコメントありブクマの最低取得件数 */
        const val LEAST_COMMENTS_NUM = 10
    }

    /** はてなアクセス用クライアント */
    val client = accountLoader.client

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

    /** エントリのスター情報 */
    val entryStarsEntry by lazy {
        MutableLiveData<StarsEntry?>()
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

    /** 新着ブクマの続きを読み込めるかどうか */
    val additionalLoadable : Boolean
        get() = recentCursor != null

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
    // カスタムタブの設定

    /** 「カスタム」タブのリストに表示するユーザータグ(ID) */
    val customBookmarksActiveTagIds by lazy {
        PreferenceLiveData(prefs, PreferenceKey.CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS) { p, key ->
            p.getObject<List<Int>>(key)
        }
    }

    /** 「カスタム」タブでユーザータグ未分類のユーザーを表示する */
    val showUnaffiliatedUsersInCustomBookmarks by lazy {
        PreferenceLiveData(prefs, PreferenceKey.CUSTOM_BOOKMARKS_IS_UNAFFILIATED_USERS_ACTIVE) { p, key ->
            p.getBoolean(key)
        }
    }

    /** 「カスタム」タブで無言ブクマを表示する */
    val showNoCommentUsersInCustomBookmarks by lazy {
        PreferenceLiveData(prefs, PreferenceKey.CUSTOM_BOOKMARKS_IS_NO_COMMENT_USERS_ACTIVE) { p, key ->
            p.getBoolean(key)
        }
    }

    /** 「カスタム」タブで非表示ユーザーを表示する */
    val showMutedUsersInCustomBookmarks by lazy {
        PreferenceLiveData(prefs, PreferenceKey.CUSTOM_BOOKMARKS_IS_MUTED_USERS_ACTIVE) { p, key ->
            p.getBoolean(key)
        }
    }

    // ------ //

    /** スター付与ポップアップを使用する */
    val useAddStarPopupMenu : Boolean by lazy {
        prefs.getBoolean(PreferenceKey.BOOKMARKS_USE_ADD_STAR_POPUP_MENU)
    }

    /** リンクをクリックしたときの処理 */
    val linkSingleTapEntryAction by lazy {
        TapEntryAction.fromId(prefs.getInt(PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION))
    }

    /** リンクを長押ししたときの処理 */
    val linkLongTapEntryAction by lazy {
        TapEntryAction.fromId(prefs.getInt(PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION))
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
                // エントリにつけられたスター
                async {
                    runCatching {
                        getStarsEntry(modifiedUrl, forceUpdate = true).let {
                            withContext(Dispatchers.Main) {
                                it.observeForever { starsEntry ->
                                    entryStarsEntry.value = starsEntry
                                }
                            }
                        }
                    }
                },
                // 全ブクマ情報を含むエントリ
                async {
                    runCatching {
                        loadBookmarksEntry(modifiedUrl)
                    }
                },
                // 人気ブクマ
                async {
                    runCatching {
                        loadPopularBookmarks()
                    }
                },
                // 新着ブクマ
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
    suspend fun loadEntry(url: String) : Entry {
        val result = runCatching {
            getEntry(url)
        }

        val e = result.getOrThrow()

        withContext(Dispatchers.Main) {
            entry.value = e
        }

        return e
    }

    /**
     * エントリ情報をロードする
     *
     * @throws ConnectionFailureException
     */
    suspend fun loadEntry(eid: Long) : Entry {
        val result = runCatching {
            getEntry(eid)
        }

        val e = result.getOrThrow()

        withContext(Dispatchers.Main) {
            entry.value = e
        }

        return e
    }

    /**
     * エントリ情報を取得する(リポジトリにロードはしない)
     *
     * @throws ConnectionFailureException
     */
    suspend fun getEntry(url: String) : Entry {
        val result = runCatching {
            client.getEntryAsync(url).await()
        }
        return result.getOrElse { throw ConnectionFailureException(it) }
    }

    /**
     * エントリ情報を取得する(リポジトリにロードはしない)
     *
     * @throws ConnectionFailureException
     */
    suspend fun getEntry(eid: Long) : Entry {
        val result = runCatching {
            client.getEntryAsync(eid).await()
        }
        return result.getOrElse { throw ConnectionFailureException(it) }
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

    /**
     * Intentで渡された情報からエントリを読み込み、ブクマリストを初期化する
     *
     * @throws IllegalArgumentException
     * @throws kotlinx.coroutines.JobCancellationException
     */
    suspend fun loadEntryFromIntent(intent: Intent) {
        val entry = intent.getObjectExtra<Entry>(EXTRA_ENTRY)
        if (entry != null) {
            loadEntry(entry)
            loadBookmarks(entry.url)
            return
        }

        val eid = intent.getLongExtra(EXTRA_ENTRY_ID, 0L)
        if (eid > 0L) {
            val e = loadEntry(eid)
            loadBookmarks(e.url)
            return
        }

        val url = intent.getStringExtra(EXTRA_ENTRY_URL) ?: when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
        if (url != null && URLUtil.isNetworkUrl(url)) {
            val modifiedUrl = modifySpecificUrls(url) ?: url
            val e = loadEntry(modifiedUrl)
            loadBookmarks(e.url)
            return
        }

        throw IllegalArgumentException()
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

    /** カスタムタブに表示するブクマを抽出する */
    private fun filterForCustomTab(src: List<BookmarkWithStarCount>) : List<Bookmark> {
        val filterNoCommentUsers = showNoCommentUsersInCustomBookmarks.value != true
        val filterMutedUsers = showMutedUsersInCustomBookmarks.value != true
        val showUntaggedUsers = showUnaffiliatedUsersInCustomBookmarks.value == true
        val activeTagIds = customBookmarksActiveTagIds.value.orEmpty()

        return src.filter { b ->
            when {
                // コメントがない
                filterNoCommentUsers && b.comment.isBlank() -> false

                // 非表示対象
                filterMutedUsers && checkIgnored(b) -> false

                // ユーザータグがひとつもついていない or 設定したタグがついている
                // 予めユーザータグがロードされている必要がある
                // TODO: liveData化
                else -> taggedUsers[b.user].let { liveData ->
                    liveData?.value?.tags?.any { tag -> activeTagIds.any { it == tag.id } }
                        ?: showUntaggedUsers
                }
            }
        }.map { Bookmark.create(it) }
    }

    /** 新着ブクマリストに依存する各種リストに変更を通知する */
    @WorkerThread
    private suspend fun updateRecentBookmarksLiveData(
        rawList: List<BookmarkWithStarCount>
    ) = coroutineScope {
        val list = wordFilter(rawList)

        val jobs = listOf(
            launch {
                // 新着
                val items = filterIgnored(list.filter { b ->
                    b.comment.isNotBlank()
                })
                recentBookmarks.postValue(items)
            },

            launch {
                // すべて
                allBookmarks.postValue(
                    if (showIgnoredUsersInAllBookmarks) list.map { Bookmark.create(it) }
                    else filterIgnored(list)
                )
            },

            launch {
                // カスタムタブ
                customBookmarks.postValue(
                    filterForCustomTab(list)
                )
            }
        )

        jobs.joinAll()
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
            bookmarksDigestCache?.referedBlogEntries.orEmpty(),
            bookmarksDigestCache?.scoredBookmarks?.map {
                if (it.user == user) bookmark else it
            }.orEmpty(),
            bookmarksDigestCache?.favoriteBookmarks?.map {
                if (it.user == user) bookmark else it
            }.orEmpty()
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
        // キャッシュに追加
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

        // BookmarksEntryのブクマリストにも追加しておく
        bookmarksEntry.value?.let { bEntry ->
            val exists = bEntry.bookmarks.filter { existed ->
                response.bookmarks.none { it.user == existed.user }
            }
            val new = response.bookmarks.map { Bookmark.create(it) }

            bookmarksEntry.postValue(
                bEntry.copy(bookmarks = exists.plus(new).sortedByDescending { it.timestamp })
            )
        }
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

                    val commentsNum = response.bookmarks.count { it.comment.isNotBlank() }

                    when {
                        cursor == null -> true

                        threshold == null -> commentsNum >= LEAST_COMMENTS_NUM

                        else -> response.bookmarks.lastOrNull { it.timestamp <= threshold } != null
                    }
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
            val result = runCatching {
                getStarsEntry(bookmark.getBookmarkUrl(entry), forceUpdate)
            }
            result.getOrNull()
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
     * ユーザーが対象ブクマにスターをつけているか確認する
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
            bookmarksDigestCache?.referedBlogEntries.orEmpty(),
            bookmarksDigestCache?.scoredBookmarks?.filterNot { it.user == user }.orEmpty(),
            bookmarksDigestCache?.favoriteBookmarks?.filterNot { it.user == user }.orEmpty()
        )

        bookmarksRecentCache =
            bookmarksRecentCache.filterNot { it.user == user }

        refreshBookmarks()
    }

    // ------ //

    /** 指定ブクマに言及しているブクマを取得する */
    fun getMentionsTo(bookmark: Bookmark) : List<Bookmark> {
        val mentionRegex = Regex("""(id\s*:|>)\s*\Q${bookmark.user}\E""")
        return bookmarksEntry.value?.bookmarks?.filter { it.comment.contains(mentionRegex) }.orEmpty()
    }

    /** 指定ブクマが言及しているブクマを取得する */
    fun getMentionsFrom(bookmark: Bookmark) : List<Bookmark> {
        val mentionRegex = Regex("""(id\s*:|>)\s*([A-Za-z0-9_]+)""")
        val matches = mentionRegex.findAll(bookmark.comment)
        val ids = matches.map { it.groupValues[2] }
        return bookmarksEntry.value?.bookmarks?.filter { ids.contains(it.user) }.orEmpty()
    }

    /** 指定ブクマにつけられたスターとそれをつけたユーザーのブクマを取得する */
    suspend fun getStarRelationsTo(
        bookmark: Bookmark,
        forceUpdate: Boolean = false
    ) : List<StarRelation> = withContext(Dispatchers.Default) {
        val receiver = bookmark.user
        val starsEntry = getStarsEntry(bookmark, forceUpdate)

        starsEntry?.value?.allStars?.map { star ->
            val sender = star.user
            StarRelation(
                sender,
                receiver,
                senderBookmark = bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == sender},
                receiverBookmark = bookmark,
                star = star
            )
        }.orEmpty()
    }

    /** 指定ブクマのユーザーがつけたスターとブクマを取得する */
    suspend fun getStarRelationsFrom(
        bookmark: Bookmark,
        forceUpdate: Boolean = false
    ) : List<StarRelation> = withContext(Dispatchers.Default) {
        if (forceUpdate) {
            entry.value?.let { entry ->
                bookmarksEntry.value?.bookmarks
                    ?.filter { it.comment.isNotBlank() }
                    ?.map {
                        it.getBookmarkUrl(entry)
                    }?.let { urls ->
                        loadStarsEntries(urls, forceUpdate = true)
                    }
            }
        }

        val userNameRegex = Regex("""\Q${HatenaClient.B_BASE_URL}\E/([A-Za-z0-9_]+)/.*""")
        val sender = bookmark.user
        val starsEntries = getUserStars(sender)

        starsEntries.mapNotNull { liveData ->
            val starsEntry = liveData.value ?: return@mapNotNull null
            val star = starsEntry.allStars.firstOrNull { it.user == sender } ?: return@mapNotNull null
            val match = userNameRegex.find(starsEntry.url)
            val receiver = match?.groupValues?.get(1) ?: return@mapNotNull null
            StarRelation(
                sender,
                receiver,
                senderBookmark = bookmark,
                receiverBookmark = bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == receiver}
                    ?: return@mapNotNull  null,
                star = star
            )
        }
    }
}
