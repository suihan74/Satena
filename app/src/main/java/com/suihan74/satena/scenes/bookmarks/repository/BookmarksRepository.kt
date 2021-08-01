package com.suihan74.satena.scenes.bookmarks.repository

import android.content.Intent
import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.CustomDigestSettingsKey
import com.suihan74.satena.models.EntryReadActionType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.models.userTag.UserTagDao
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.satena.scenes.bookmarks.TapTitleBarAction
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepository
import com.suihan74.satena.scenes.preferences.ignored.UserRelationRepository
import com.suihan74.satena.scenes.preferences.ignored.UserRelationRepositoryInterface
import com.suihan74.utilities.*
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getObjectExtra
import com.suihan74.utilities.extensions.updateFirstOrPlusAhead
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * ブクマ画面用のリポジトリ
 */
class BookmarksRepository(
    val accountLoader: AccountLoader,
    val prefs : SafeSharedPreferences<PreferenceKey>,
    val customDigestSettings : SafeSharedPreferences<CustomDigestSettingsKey>,
    val ignoredEntriesRepo : IgnoredEntriesRepository,
    private val userTagDao: UserTagDao,
) :
        // ユーザー関係
        UserRelationRepositoryInterface by UserRelationRepository(accountLoader),
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
        const val LEAST_COMMENTS_NUM = 3
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
    val entry = MutableLiveData<Entry?>()

    /** ブクマを含むエントリ情報 */
    val bookmarksEntry = MutableLiveData<BookmarksEntry?>()

    /** エントリのスター情報 */
    val entryStarsEntry = MutableLiveData<StarsEntry?>()

    // 各タブでの表示用のブクマリスト

    private val lazyBookmarksList
        get() = lazy {
            MutableLiveData<List<Bookmark>>()
        }

    /** 新着ブクマ */
    val recentBookmarks by lazyBookmarksList

    /** 全ブクマ */
    val allBookmarks by lazyBookmarksList

    /** カスタム */
    val customBookmarks by lazyBookmarksList

    // 取得したブクマデータのキャッシュ

    /** 人気ブクマ、関連記事、お気に入りユーザーのブクマ */
    private val _bookmarksDigest = MutableLiveData<BookmarksDigest?>()
    val bookmarksDigest : LiveData<BookmarksDigest?> = _bookmarksDigest

    val popularBookmarks : List<Bookmark>
        get() = filterIgnored(
            wordFilter(
                bookmarksDigest.value?.scoredBookmarks.orEmpty()
            )
        )

    val followingsBookmarks : List<Bookmark>
        get() = filterIgnored(
            wordFilter(
                bookmarksDigest.value?.favoriteBookmarks.orEmpty()
            )
        )

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
    // ダイジェスト抽出設定

    /** 最大要素数 */
    val maxNumOfElements by lazy {
        PreferenceLiveData(customDigestSettings, CustomDigestSettingsKey.MAX_NUM_OF_ELEMENTS) { p, key ->
            p.getInt(key)
        }
    }

    /** 抽出対象になるスター数の閾値 */
    val starsCountThreshold by lazy {
        PreferenceLiveData(customDigestSettings, CustomDigestSettingsKey.STARS_COUNT_THRESHOLD) { p, key ->
            p.getInt(key)
        }
    }

    /** カスタムダイジェストを使用する */
    val useCustomDigest by lazy {
        PreferenceLiveData(customDigestSettings, CustomDigestSettingsKey.USE_CUSTOM_DIGEST) { p, key ->
            p.getBoolean(key)
        }
    }

    /** 非表示ユーザーのスターを無視する */
    val ignoreStarsByIgnoredUsers by lazy {
        PreferenceLiveData(customDigestSettings, CustomDigestSettingsKey.IGNORE_STARS_BY_IGNORED_USERS) { p, key ->
            p.getBoolean(key)
        }
    }

    /** 同じユーザーが複数つけた同色のスターを1個だけと数える */
    val deduplicateStars by lazy {
        PreferenceLiveData(customDigestSettings, CustomDigestSettingsKey.DEDUPLICATE_STARS) { p, key ->
            p.getBoolean(key)
        }
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

    val titleBarSingleClickBehavior by lazy {
        PreferenceLiveData(prefs, PreferenceKey.BOOKMARKS_TITLE_SINGLE_CLICK_BEHAVIOR) { p, key ->
            TapTitleBarAction.fromId(p.getInt(key))
        }
    }

    val titleBarLongClickBehavior by lazy {
        PreferenceLiveData(prefs, PreferenceKey.BOOKMARKS_TITLE_LONG_CLICK_BEHAVIOR) { p, key ->
            TapTitleBarAction.fromId(p.getInt(key))
        }
    }

    // ------ //

    private val _staticLoading = MutableLiveData<Boolean>()
    /** 画面を停止して行うべき読み込みの発生状態 */
    val staticLoading : LiveData<Boolean> = _staticLoading

    suspend fun startLoading() = withContext(Dispatchers.Main) {
        _staticLoading.value = true
    }

    suspend fun stopLoading() = withContext(Dispatchers.Main) {
        _staticLoading.value = false
    }

    // ------ //

    suspend fun clear() = withContext(Dispatchers.Main) {
        loadingBookmarksEntryMutex.withLock {
            _staticLoading.value = false
            entry.value = null
            bookmarksEntry.value = null
            entryStarsEntry.value = null
            bookmarksRecentCache = emptyList()
            _bookmarksDigest.value = null
            recentBookmarks.value = null
            allBookmarks.value = null
            customBookmarks.value = null
        }
    }

    /**
     * URLを渡して必要な初期化を行う
     *
     * @throws TaskFailureException
     */
    suspend fun loadBookmarks(url: String) = withContext(Dispatchers.Default) {
        startLoading()

        val modifyResult = runCatching {
            modifySpecificUrls(url)
        }
        val modifiedUrl = modifyResult.getOrNull() ?: url
        this@BookmarksRepository.url = modifiedUrl

        bookmarksRecentCache = emptyList()

        try {
            val loadingIgnoresTasks = listOf(
                async { ignoredEntriesRepo.loadIgnoredWordsForBookmarks() },
                async { loadIgnoredUsers() }
            )
            loadingIgnoresTasks.awaitAll()
            loadEntry(modifiedUrl)

            var forbiddenException: ForbiddenException? = null
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
                // 全ブクマ情報を含むエントリとダイジェスト
                async {
                    loadBookmarksEntry(modifiedUrl)
                    try {
                        loadPopularBookmarks()
                    }
                    catch (e: ForbiddenException) {
                        forbiddenException = e
                    }
                },
                // 新着ブクマ
                async {
                    try {
                        loadRecentBookmarks(additionalLoading = false)
                    }
                    catch (e: ForbiddenException) {
                        forbiddenException = e
                    }
                }
            )
            loadingContentsTasks.awaitAll()

            forbiddenException?.let {
                throw it
            }
        }
        catch (e: Throwable) {
            Log.e("BookmarksRepo", Log.getStackTraceString(e))
            throw TaskFailureException(cause = e)
        }
        finally {
            stopLoading()
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
        startLoading()
        val result = runCatching {
            client.getEntryAsync(url).await()
        }
        stopLoading()
        return result.getOrElse { throw ConnectionFailureException(it) }
    }

    /**
     * エントリ情報を取得する(リポジトリにロードはしない)
     *
     * @throws ConnectionFailureException
     */
    suspend fun getEntry(eid: Long) : Entry {
        startLoading()
        val result = runCatching {
            client.getEntryAsync(eid).await()
        }
        stopLoading()
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
                insertMyBookmark(
                    client.getBookmarksEntryAsync(url).await()
                )
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
     * ブクマエントリにユーザーの非公開ブクマを挿入する
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun insertMyBookmark(bookmarksEntry: BookmarksEntry) : BookmarksEntry {
        val bookmarkData = entry.value?.bookmarkedData ?: return bookmarksEntry
        val user = bookmarkData.user
        return if (bookmarksEntry.bookmarks.none { it.user == user }) {
            val bookmarks = buildList {
                if (bookmarksEntry.bookmarks.isEmpty()) {
                    add(Bookmark.create(bookmarkData))
                }
                else {
                    val timestamp = bookmarkData.timestamp
                    val insertIdx = bookmarksEntry.bookmarks.indexOfFirst { it.timestamp <= timestamp }
                    addAll(bookmarksEntry.bookmarks)
                    add(insertIdx, Bookmark.create(bookmarkData))
                }
            }
            bookmarksEntry.copy(bookmarks = bookmarks)
        }
        else bookmarksEntry
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
                is ForbiddenException,
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

        val keywords = keywordsRaw.lowercase().split(Regex("""\s+"""))
        val keywordsRegex = Regex(keywords.joinToString(separator = "|") { Regex.escape(it) })

        return src.filter { b ->
            keywordsRegex.containsMatchIn(b.string.lowercase())
        }
    }

    /** カスタムタブに表示するブクマを抽出する */
    private suspend fun filterForCustomTab(src: List<BookmarkWithStarCount>) : List<Bookmark> {
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
                else -> getUserTags(b.user).let { liveData ->
                    val tags = liveData.value?.tags.orEmpty()
                    showUntaggedUsers && tags.isEmpty() ||
                            tags.any { tag -> activeTagIds.any { it == tag.id } }
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

            launch(SupervisorJob()) {
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
        _bookmarksDigest.postValue(_bookmarksDigest.value)
        // 新着ブクマリストを再生成
        updateRecentBookmarksLiveData(bookmarksRecentCache)
    }

    /** 他の画面から復帰時にキャッシュを再読み込みする */
    suspend fun onRestart() {
        loadIgnoredUsers()
        loadUserTags()

        val users = bookmarksDigest.value?.scoredBookmarks?.map { it.user }
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

        val digest = bookmarksDigest.value
        _bookmarksDigest.postValue(BookmarksDigest(
            digest?.referedBlogEntries.orEmpty(),
            digest?.scoredBookmarks?.map {
                if (it.user == user) bookmark else it
            }.orEmpty(),
            digest?.favoriteBookmarks?.map {
                if (it.user == user) bookmark else it
            }.orEmpty()
        ))

        bookmarksRecentCache =
            bookmarksRecentCache.updateFirstOrPlusAhead(bookmark) { it.user == user }

        updateRecentBookmarksLiveData(bookmarksRecentCache)
        // refreshBookmarks()
    }

    /**
     * 指定ブクマのスター数を更新する
     */
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun updateStarCounts(bookmark: Bookmark) {
        val starsEntry = getStarsEntry(bookmark).value ?: return
        val user = bookmark.user
        val predicate : (BookmarkWithStarCount)->Boolean = { it.user == user }
        val targetBookmark = bookmarksDigest.value?.scoredBookmarks?.firstOrNull(predicate)
            ?: bookmarksDigest.value?.favoriteBookmarks?.firstOrNull(predicate)
            ?: bookmarksRecentCache.firstOrNull(predicate)
            ?: return

        val bookmarkWithStarCount = targetBookmark.copy(starCount = buildList {
            appendStarCount(starsEntry, StarColor.Yellow)
            appendStarCount(starsEntry, StarColor.Red)
            appendStarCount(starsEntry, StarColor.Green)
            appendStarCount(starsEntry, StarColor.Blue)
            appendStarCount(starsEntry, StarColor.Purple)
        })
        updateBookmark(bookmarkWithStarCount)
    }

    private fun MutableList<StarCount>.appendStarCount(starsEntry: StarsEntry, color: StarColor) {
        val count = starsEntry.getStarsCount(color)
        if (count > 0) {
            this.add(StarCount(color, count))
        }
    }

    /**
     * 人気ブクマリストを取得する
     *
     * @throws ConnectionFailureException
     * @throws ForbiddenException
     * @throws TimeoutException
     */
    suspend fun loadPopularBookmarks() = withContext(Dispatchers.Default) {
        val result = runCatching {
            client.getDigestBookmarksAsync(url).await().let { digest ->
                if (useCustomDigest.value == true) {
                    val userScoredBookmarks = loadUserCustomizedDigest()
                    digest.copy(scoredBookmarks = userScoredBookmarks)
                }
                else digest
            }
        }

        val digest = result.getOrElse {
            when (it) {
                is TimeoutException,
                is ForbiddenException ->
                    throw it

                else -> throw ConnectionFailureException(it)
            }
        }

        _bookmarksDigest.postValue(digest)
        val bookmarks = filterIgnored(digest.favoriteBookmarks.plus(digest.scoredBookmarks).distinctBy { it.user })

        bookmarks.forEach {
            loadUserTags(it.user)
        }

        // ブクマへのブクマ数を取得する
        loadBookmarkCountsToBookmarks(bookmarks)

        // スター情報を取得する
        loadStarsEntriesForBookmarks(bookmarks)
    }

    /**
     *  新着ブクマリストを取得する
     *
     * @throws ConnectionFailureException
     * @throws TimeoutException
     * @throws ForbiddenException
     */
    @OptIn(ExperimentalStdlibApi::class)
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

        // 非公開ブクマを適切な位置に挿入
        val myBookmark = entry.value?.bookmarkedData

        val cacheTail = bookmarksRecentCache.lastOrNull()
        val myBookmarkNotCached =
            myBookmark != null && (cacheTail == null || cacheTail.timestamp > myBookmark.timestamp)

        val fetchedTail = response.bookmarks.lastOrNull()
        val myBookmarkFetched =
            myBookmark != null && (fetchedTail == null || response.bookmarks.none { it.user == myBookmark.user } && fetchedTail.timestamp < myBookmark.timestamp)

        val bookmarks =
            if (myBookmark != null && myBookmarkNotCached && myBookmarkFetched) buildList {
                if (response.bookmarks.isEmpty()) {
                    add(myBookmark.toBookmarkWithStarCount())
                }
                else {
                    val insertIdx = response.bookmarks.indexOfFirst { it.timestamp < myBookmark.timestamp }
                    addAll(response.bookmarks)
                    add(insertIdx, myBookmark.toBookmarkWithStarCount())
                }
            }
            else response.bookmarks

        // キャッシュに追加
        // 重複がある場合は新しい方に更新する
        bookmarksRecentCache = bookmarksRecentCache
            .filterNot { existed -> bookmarks.any { b -> existed.user == b.user } }
            .plus(bookmarks)
            .sortedByDescending { it.timestamp }

        // ユーザータグを更新
        bookmarks.forEach {
            loadUserTags(it.user)
        }

        // ブクマへのブクマ数を取得する
        loadBookmarkCountsToBookmarksWithStarCount(bookmarks)

        // スター情報を取得する
        loadStarsEntriesForBookmarksWithStarCount(bookmarks)

        // 各表示用リストに変更を通知する
        updateRecentBookmarksLiveData(bookmarksRecentCache)

        loadingBookmarksEntryMutex.withLock {
            // BookmarksEntryのブクマリストにも追加しておく
            bookmarksEntry.value?.let { bEntry ->
                val exists = bEntry.bookmarks.filter { existed ->
                    bookmarks.none { it.user == existed.user }
                }
                val new = bookmarks.map { Bookmark.create(it) }

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
     * @throws ForbiddenException
     * @throws TimeoutException
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

    /**
     * ユーザー定義のブックマークダイジェストを生成する
     */
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun loadUserCustomizedDigest() : List<BookmarkWithStarCount> {
        val maxNumOfElements = maxNumOfElements.value ?: return emptyList()
        val bookmarks = bookmarksEntry.value?.bookmarks.orEmpty()
            .filterNot { it.comment.isBlank() }
            .filterNot { checkIgnored(it) }
        loadStarsEntriesForBookmarks(bookmarks)
        val stars = bookmarks.map {
            runCatching { getStarsEntry(it).value }.getOrNull()
        }

        val targetBookmarks = bookmarks
            .mapIndexedNotNull { idx, b ->
                val s = stars[idx] ?: return@mapIndexedNotNull null
                b.copy(starCount = s.allStars)
            }
        val targetStars = stars.filterNotNull()

        val maxStarsCount =
            runCatching {
                targetStars.maxOf { it.totalStarsCount }
            }.onFailure {
                Log.e("maxStarsCount", targetStars.size.toString())
                Log.e("stars", stars.size.toString())
                Log.e("stars(not null)", stars.filterNotNull().size.toString())
                Log.e("bookmarks", bookmarks.size.toString())
            }.getOrDefault(0)
        if (maxStarsCount == 0) return emptyList()

        val scores = targetBookmarks.mapIndexed { idx, b ->
            scoreBookmark(b, targetStars[idx], maxStarsCount)
        }

        return targetBookmarks.zip(scores)
            .filter { it.second != null }
            .sortedByDescending { it.second }
            .take(maxNumOfElements)
            .map { it.first.toBookmarkWithStarCount(entry.value!!) }
    }

    fun scoreBookmark(
        bookmark: Bookmark,
        starsEntry: StarsEntry,
        maxStarsCount: Int
    ) : Double? {
        val deduplicateStars = deduplicateStars.value == true
        val ignoreStarsByIgnoredUsers = ignoreStarsByIgnoredUsers.value == true
        val colorStarsWeight = 1.0
        val starsCountThreshold = starsCountThreshold.value ?: return null
        //val commentScoreWeight = 0  // TODO: コメントそのものを評価するか検討

        val fixedStarsEntry =
            if (ignoreStarsByIgnoredUsers) starsEntry.copy(
                    stars = starsEntry.stars.filterNot { checkIgnoredUser(it.user) },
                    coloredStars = starsEntry.coloredStars?.map { group ->
                        ColorStars(
                            group.stars.filterNot { checkIgnoredUser(it.user) },
                            group.color
                        )
                    }
                )
            else starsEntry

        val yellowStarsCount =
            if (deduplicateStars) fixedStarsEntry.stars
                .distinctBy { it.user }
                .count()
            else fixedStarsEntry.getStarsCount(StarColor.Yellow)

        val colorStarsCount =
            if (deduplicateStars) fixedStarsEntry.coloredStars
                ?.flatMap { it.stars }
                ?.distinctBy { it.user }
                ?.count() ?: 0
            else fixedStarsEntry.coloredStars?.sumOf { it.starsCount } ?: 0

        return if (yellowStarsCount + colorStarsCount < starsCountThreshold) null
            else (yellowStarsCount + colorStarsCount * colorStarsWeight) / maxStarsCount

//        val commentLength = bookmark.comment.length
//        val starScore = (yellowStarsCount + colorStarsCount * colorStarsWeight) / maxStarsCount
//        val commentScore = 1.0 / commentLength
//        return starScore + commentScore * commentScoreWeight
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

    private val bookmarkCounts = HashMap<String, LiveData<Int>>()
    private val bookmarkCountsMutex = Mutex()

    private suspend fun loadBookmarkCountsToBookmarksWithStarCount(bookmarks: List<BookmarkWithStarCount>) {
        loadBookmarkCountsToBookmarksImpl(bookmarks.map { it.user })
    }

    private suspend fun loadBookmarkCountsToBookmarks(bookmarks: List<Bookmark>) {
        loadBookmarkCountsToBookmarksImpl(bookmarks.map { it.user })
    }

    /**
     * ブクマへのブクマ数を取得する
     */
    private suspend fun loadBookmarkCountsToBookmarksImpl(users: List<String>) = withContext(Dispatchers.Default) {
        val entry = entry.value ?: return@withContext
        if (entry.id == 0L) return@withContext
        val bookmarkUrls = users.map { user ->
            bookmarkCountsMutex.withLock {
                bookmarkCounts.getOrPut(user) { MutableLiveData() }
            }
            HatenaClient.getBookmarkCommentUrl(entry.id, user)
        }

        launch {
            runCatching {
                val map = HatenaClient.getBookmarkCountsAsync(bookmarkUrls).await()
                bookmarkCountsMutex.withLock {
                    users.forEach { user ->
                        val url = HatenaClient.getBookmarkCommentUrl(entry.id, user)
                        bookmarkCounts[user].alsoAs<MutableLiveData<Int>> { liveData ->
                            map[url]?.let {
                                liveData.postValue(it)
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun getBookmarkCounts(bookmark: Bookmark) : LiveData<Int> = withContext(Dispatchers.Main) {
        return@withContext bookmarkCountsMutex.withLock {
            bookmarkCounts.getOrPut(bookmark.user) { MutableLiveData() }
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
            runCatching {
                loadStarsEntries(bookmarkUrls)
            }
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
            runCatching {
                loadStarsEntries(bookmarkUrls)
            }
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

        startLoading()

        val result = runCatching {
            client.deleteBookmarkAsync(url).await()
        }

        if (result.isFailure) {
            stopLoading()
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

        val digest = bookmarksDigest.value
        _bookmarksDigest.postValue(BookmarksDigest(
            digest?.referedBlogEntries.orEmpty(),
            digest?.scoredBookmarks?.filterNot { it.user == user }.orEmpty(),
            digest?.favoriteBookmarks?.filterNot { it.user == user }.orEmpty()
        ))

        bookmarksRecentCache =
            bookmarksRecentCache.filterNot { it.user == user }

        refreshBookmarks()
        stopLoading()
    }

    // ------ //

    /**
     * (リンクを)あとで読む
     *
     * @throws TaskFailureException
     */
    suspend fun readLater(entry: Entry) {
        requireSignIn()
        startLoading()
        val result = runCatching {
            client.postBookmarkAsync(entry.url, readLater = true).await()
        }
        stopLoading()

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

    /**
     * 指定ブクマのユーザーがつけたスターとブクマを取得する
     *
     * @throws TaskFailureException
     */
    suspend fun getStarRelationsFrom(
        bookmark: Bookmark,
        forceUpdate: Boolean = false
    ) : List<StarRelation> = withContext(Dispatchers.Default) {
        runCatching {
            entry.value?.let { entry ->
                bookmarksEntry.value?.bookmarks
                    ?.filter { it.comment.isNotBlank() }
                    ?.map {
                        it.getBookmarkUrl(entry)
                    }?.let { urls ->
                        loadStarsEntries(urls, forceUpdate = forceUpdate)
                    }
            }
        }.onFailure {
            throw TaskFailureException(cause = it)
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
