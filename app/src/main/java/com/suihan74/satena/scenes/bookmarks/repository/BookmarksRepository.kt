package com.suihan74.satena.scenes.bookmarks.repository

import android.content.Intent
import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.EntryReadActionType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.models.userTag.UserTagDao
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersRepositoryInterface
import com.suihan74.utilities.*
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.getObjectExtra
import com.suihan74.utilities.extensions.updateFirstOrPlusAhead
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ブクマ画面用のリポジトリ
 */
class BookmarksRepository(
    val accountLoader: AccountLoader,
    val prefs : SafeSharedPreferences<PreferenceKey>,
    val ignoredEntriesRepo : IgnoredEntriesRepository,
    private val userTagDao: UserTagDao
) :
        // ユーザー非表示
        IgnoredUsersRepositoryInterface by IgnoredUsersRepository(accountLoader),
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

    /**
     * アカウントが必要な操作前にサインインする
     *
     * @throws SignInFailureException サインイン失敗
     */
    suspend fun signIn() : Account? = withContext(Dispatchers.Default) {
        val result = runCatching {
            accountLoader.signInAccounts(reSignIn = false)
        }

        if (result.isFailure) {
            throw SignInFailureException(cause = result.exceptionOrNull())
        }

        val account = client.account
        val signedIn = account != null

        this@BookmarksRepository.signedIn.postValue(signedIn)

        if (signedIn) {
            runCatching {
                loadUserColorStarsCount()
            }
        }

        return@withContext account
    }

    /**
     * サインインが必要な処理
     *
     * @throws SignInFailureException サインインされていない
     */
    suspend fun requireSignIn() {
        signIn()
        if (client.account == null) {
            throw SignInStarFailureException()
        }
    }

    // ------ //

    /**
     * ブクマエントリ情報のロード完了前に，それに依存するブクマリストの加工を始めないようにするためのロック
     */
    private val loadingBookmarksEntryMutex = Mutex()

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
     *
     * @throws TaskFailureException
     */
    suspend fun loadBookmarks(url: String) = withContext(Dispatchers.Default) {
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
                async { ignoredEntriesRepo.loadIgnoredWordsForBookmarks() },
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
                    loadBookmarksEntry(modifiedUrl)
                },
                // 人気ブクマ
                async {
                    loadPopularBookmarks()
                },
                // 新着ブクマ
                async {
                    loadRecentBookmarks(additionalLoading = false)
                }
            )
            loadingContentsTasks.awaitAll()
        }
        catch (e: Throwable) {
            Log.e("BookmarksRepo", Log.getStackTraceString(e))
            throw TaskFailureException(cause = e)
        }
        finally {
            withContext(Dispatchers.Main) {
                loadingEntry.value = false
            }
        }
    }

    /** 取得済みのエントリ情報をセットする */
    private suspend fun loadEntry(e: Entry) = withContext(Dispatchers.Main.immediate) {
        entry.value = e
    }

    /**
     * エントリ情報をロードする
     *
     * @throws ConnectionFailureException
     */
    private suspend fun loadEntry(url: String) : Entry {
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
    private suspend fun loadEntry(eid: Long) : Entry {
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
        loadingBookmarksEntryMutex.withLock {
            val result = runCatching {
                client.getBookmarksEntryAsync(url).await()
            }

            val e = result.getOrElse {
                throw ConnectionFailureException(it)
            }

            withContext(Dispatchers.Main.immediate) {
                bookmarksEntry.value = e
            }
        }
    }

    /**
     * Intentで渡された情報からエントリを読み込み、ブクマリストを初期化する
     *
     * @throws ConnectionFailureException - 通信失敗
     * @throws NotFoundException - エントリ情報が取得できない
     * @throws IllegalArgumentException - インテントに正しいパラメータが設定されていない
     */
    suspend fun loadEntryFromIntent(intent: Intent) {
        val onLoadBookmarksFailure: (Throwable)->Unit = {
            if (it is CancellationException) throw it
            when (val e = it.cause) {
                is CancellationException,
                is ConnectionFailureException,
                is NotFoundException ->
                    throw e

                else -> throw ConnectionFailureException(cause = e)
            }
        }

        val entry = intent.getObjectExtra<Entry>(EXTRA_ENTRY)
        if (entry != null) {
            loadEntry(entry)
            runCatching {
                loadBookmarks(entry.url)
            }
            .onFailure(onLoadBookmarksFailure)
            return
        }

        val eid = intent.getLongExtra(EXTRA_ENTRY_ID, 0L)
        if (eid > 0L) {
            runCatching {
                val e = loadEntry(eid)
                loadBookmarks(e.url)
            }
            .onFailure(onLoadBookmarksFailure)
            return
        }

        val url = intent.getStringExtra(EXTRA_ENTRY_URL) ?: when (intent.action) {
            Intent.ACTION_VIEW -> {
                HatenaClient.getEntryUrlFromCommentPageUrl(intent.dataString.orEmpty())
            }

            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)

            else -> null
        }
        if (url != null && URLUtil.isNetworkUrl(url)) {
            val modifiedUrl = modifySpecificUrls(url) ?: url
            val e = loadEntry(modifiedUrl)
            runCatching {
                loadBookmarks(e.url)
            }
            .onFailure(onLoadBookmarksFailure)

            return
        }

        throw IllegalArgumentException()
    }

    // ------ //

    /**
     * ユーザーが非表示ユーザーであるかを判別する
     *
     * はてなの「非表示ユーザー」として登録されているかだけを判別する
     */
    fun checkIgnoredUser(user: String) : Boolean {
        return ignoredUsersCache.contains(user)
    }

    /**
     * ユーザーが非表示対象かを判別する
     *
     * `checkIgnoredUser(user)`との違いは，非表示テキストにマッチするかも確認すること。
     * はてなの「非表示ユーザー」設定が有効かどうかを確認する場合には`checkIgnoredUser(user)`を使用する
     */
    private fun checkIgnored(user: String) : Boolean {
        if (ignoredUsersCache.contains(user)) return true
        return ignoredEntriesRepo.ignoredWordsForBookmarks.any { w -> user.contains(w) }
    }

    /** ブクマが非表示対象かを判別する */
    fun checkIgnored(bookmark: Bookmark) : Boolean {
        if (ignoredUsersCache.any { bookmark.user == it }) return true
        return ignoredEntriesRepo.ignoredWordsForBookmarks.any { w ->
            bookmark.commentRaw.contains(w)
                    || bookmark.user.contains(w)
                    || bookmark.tags.any { t -> t.contains(w) }
        }
    }

    /** ブクマが非表示対象かを判別する */
    private fun checkIgnored(bookmark: BookmarkWithStarCount) : Boolean {
        if (ignoredUsersCache.any { bookmark.user == it }) return true
        return ignoredEntriesRepo.ignoredWordsForBookmarks.any { w ->
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
    private suspend fun updateRecentBookmarksLiveData(
        rawList: List<BookmarkWithStarCount>
    ) = withContext(Dispatchers.Default) {
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

        val digest = result.getOrElse { bookmarksDigestCache }

        bookmarksDigestCache = digest
        val bookmarks = filterIgnored(digest?.scoredBookmarks.orEmpty())
        bookmarks.forEach {
            loadUserTags(it.user)
        }

        popularBookmarks.postValue(bookmarks)

        result.onFailure {
            when (it) {
                is TimeoutException, is NotFoundException ->
                    throw it

                else ->
                    throw ConnectionFailureException(it)
            }
        }

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
                client.getRecentBookmarksAsync(url, cursor = recentCursor).await()
            }
            else {
                loadMostRecentBookmarks(url)
            }
        }

        if (result.isFailure) {
            updateRecentBookmarksLiveData(bookmarksRecentCache)
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

        // スター情報を取得する
        loadStarsEntriesForBookmarksWithStarCount(response.bookmarks)

        // 各表示用リストに変更を通知する
        updateRecentBookmarksLiveData(bookmarksRecentCache)

        loadingBookmarksEntryMutex.withLock {
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
    }

    /**
     * 最新のブクマリストを(取得済みの位置まで)取得する
     *
     * @throws ConnectionFailureException
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
                is Throwable -> throw e
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
     * @throws TaskFailureException
     */
    suspend fun getStarsEntry(bookmark: Bookmark, forceUpdate: Boolean = false) : LiveData<StarsEntry> {
        val result = runCatching {
            getStarsEntry(bookmark.getBookmarkUrl(entry.value!!), forceUpdate)
        }
        .onFailure {
            throw TaskFailureException(cause = it)
        }

        return result.getOrNull()!!
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
        try {
            val starsEntry = getStarsEntry(bookmark, forceUpdate = false).value
            return starsEntry?.allStars?.filter { it.user == user }
        }
        catch (e: Throwable) {
            return null
        }
    }

    // ------ //

    /**
     * エントリのブクマを削除する
     *
     * @throws SignInFailureException
     * @throws TaskFailureException
     */
    suspend fun deleteBookmark(entry: Entry) = withContext(Dispatchers.Default) {
        val url = entry.url

        requireSignIn()

        val result = runCatching {
            client.deleteBookmarkAsync(url).await()
        }

        if (result.isFailure) {
            throw TaskFailureException(cause = result.exceptionOrNull())
        }
    }

    /**
     * ブクマを削除する
     *
     * @throws SignInFailureException
     * @throws TaskFailureException
     */
    suspend fun deleteBookmark(bookmark: Bookmark) = withContext(Dispatchers.Default) {
        val url = entry.value?.url ?: throw TaskFailureException("invalid entry")
        val user = bookmark.user

        requireSignIn()

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

    /**
     * (リンクを)あとで読む
     *
     * @throws TaskFailureException
     */
    suspend fun readLater(entry: Entry) {
        requireSignIn()
        val result = runCatching {
            client.postBookmarkAsync(entry.url, readLater = true).await()
        }

        if (result.isFailure) {
            throw TaskFailureException(cause = result.exceptionOrNull())
        }
    }

    /**
     * (リンクを)読んだ
     */
    suspend fun read(entry: Entry) : Pair<EntryReadActionType, BookmarkResult?> {
        requireSignIn()
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

    // ------ //

    /** 指定ブクマに言及しているブクマを取得する */
    fun getMentionsTo(bookmark: Bookmark) : List<Bookmark> {
        val displayIgnoredUsers = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING)
        val mentionRegex = Regex("""(id\s*:|>)\s*\Q${bookmark.user}\E""")
        val bookmarks = bookmarksEntry.value?.bookmarks?.filter { it.comment.contains(mentionRegex) }.orEmpty()

        return if (displayIgnoredUsers) bookmarks
        else bookmarks.filter { !checkIgnored(it) }
    }

    /** 指定ブクマが言及しているブクマを取得する */
    fun getMentionsFrom(bookmark: Bookmark, analyzedBookmarkComment: AnalyzedBookmarkComment? = null) : List<Bookmark> {
        val displayIgnoredUsers = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING)

        val ids = analyzedBookmarkComment?.ids ?: let {
            val mentionRegex = Regex("""(id\s*:|>)\s*([A-Za-z0-9_-]+)""")
            val matches = mentionRegex.findAll(bookmark.comment)
            matches.map { it.groupValues[2] }.toList()
        }

        val bookmarks = bookmarksEntry.value?.bookmarks?.filter { ids.contains(it.user) }.orEmpty()

        return if (displayIgnoredUsers) bookmarks
        else bookmarks.filter { !checkIgnored(it) }
    }

    /**
     * 指定ブクマにつけられたスターとそれをつけたユーザーのブクマを取得する
     *
     * @throws TaskFailureException
     */
    suspend fun getStarRelationsTo(
        bookmark: Bookmark,
        forceUpdate: Boolean = false
    ) : List<StarRelation> = withContext(Dispatchers.Default) {
        val receiver = bookmark.user
        val starsEntry =
            runCatching { getStarsEntry(bookmark, forceUpdate) }
            .getOrThrow()

        val allStars =
            if (prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS)) starsEntry.value?.allStars
            else starsEntry.value?.allStars?.filterNot { checkIgnored(it.user) }

        allStars.orEmpty().map { star ->
            val sender = star.user
            StarRelation(
                sender,
                receiver,
                senderBookmark = bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == sender},
                receiverBookmark = bookmark,
                star = star
            )
        }
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

        val displayIgnoredUsers = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS)

        val userNameRegex = Regex("""\Q${HatenaClient.B_BASE_URL}\E/([A-Za-z0-9_]+)/.*""")
        val sender = bookmark.user
        val starsEntries = getUserStars(sender)

        starsEntries.mapNotNull { liveData ->
            val starsEntry = liveData.value ?: return@mapNotNull null
            val star = starsEntry.allStars.firstOrNull { it.user == sender } ?: return@mapNotNull null
            val match = userNameRegex.find(starsEntry.url)
            val receiver = match?.groupValues?.get(1) ?: return@mapNotNull null
            if (!displayIgnoredUsers && checkIgnored(receiver)) return@mapNotNull null

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
