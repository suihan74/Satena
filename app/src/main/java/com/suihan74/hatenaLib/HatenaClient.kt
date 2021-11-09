@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.suihan74.hatenaLib

import android.net.Uri
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.lang.reflect.Type
import java.net.HttpCookie
import java.net.SocketTimeoutException
import java.net.URI
import java.nio.charset.Charset
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.coroutines.CoroutineContext

/////////////////////////////////////////////////////////////////

object HatenaClient : BaseClient(), CoroutineScope {
    internal const val W_BASE_URL = "https://www.hatena.ne.jp"
    internal const val B_BASE_URL = "https://b.hatena.ne.jp"
    internal const val S_BASE_URL = "https://s.hatena.ne.jp"

    private val mJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.IO

    /** はてなブックマークの情報取得にログイン情報が必要なリクエストに付加するクッキー */
    private var mRk : HttpCookie? = null
    /** はてなスターの情報取得にログイン情報が必要なリクエストに付加するキー */
    private var mRksForStar : String? = null

    /** rkの値 クッキー情報の"rk=???;"の???部分 */
    val rkStr : String?
        get() = mRk?.value

    /** 現在ログイン済みのユーザ情報 */
    var account : Account? = null
        get() = synchronized(this) { field }
        private set(value) {
            synchronized(this) {
                field = value
            }
        }

    /** キャッシュされた非表示ユーザーリスト */
    var ignoredUsers : List<String> = emptyList()
        get() {
            synchronized(field) {
                return field
            }
        }
        private set(value) {
            synchronized(field) {
                field = value
            }
        }

    /** 非表示ユーザーリストをある程度の時間キャッシュする */
    private var mIgnoredUsersLastUpdated = LocalDateTime.MIN
    /** 非表示ユーザーリストのキャッシュを保持する時間 */
    private val mIgnoredUsersUpdateIntervals = Duration.ofMinutes(3)

    /**
     * HatenaClientがログイン済みか確認する
     */
    fun signedIn() : Boolean = mSignedIn //mRk != null && account != null
    private var mSignedIn : Boolean = false
        get() = synchronized(field) { field }
        set(value) {
            synchronized(field) {
                field = value
            }
        }

    /**
     * HatenaClientがはてなスターのサービスにログイン済みかを確認する
     */
    fun signedInStar() : Boolean = mSignedInStar
    private var mSignedInStar : Boolean = false
        get() = synchronized(field) { field }
        set(value) {
            synchronized(field) {
                field = value
            }
        }

    /**
     * ログイン
     */
    fun signInAsync(name: String, password: String) : Deferred<Account> = async {
        if (signedIn()) {
            signOut()
        }

        val url = "$W_BASE_URL/login"
        val params = mapOf(
            "name" to name,
            "password" to password
        )

        val response =
            try { post(url, params) }
            catch (e: Throwable) { throw SignInFailureException(e) }

        signInImpl(response)
    }

    /**
     * クッキーを使用して再サインイン
     */
    suspend fun signIn(rk: String) : Account = withContext(Dispatchers.IO) {
        if (signedIn()) {
            signOut()
        }

        val cookie = HttpCookie.parse("rk=$rk;").first().also {
            it.domain = ".hatena.ne.jp"
            it.path = "/"
        }
        cookieManager.cookieStore.add(URI(cookie.domain), cookie)

        try {
            mRk = cookie
            mSignedIn = true
            account = getAccountAsync().await()
            if (account?.login != true) throw SignInFailureException()
        }
        catch (e: Throwable) {
            mRk = null
            mSignedIn = false
            account = null
            throw SignInFailureException(e)
        }

        runCatching {
            getIgnoredUsersAsync().await()
        }

        return@withContext account!!
    }

    private suspend fun signInImpl(response: Response) : Account = response.use {
        if (!response.isSuccessful) {
            mSignedIn = false
            mSignedInStar = false
            throw SignInFailureException("connection error")
        }

        cookieManager.cookieStore.cookies.firstOrNull { it.name == "rk" }?.let { rk ->
            mRk = rk
        }

        try {
            account = getAccountAsync().await()
            mSignedIn = true
        }
        catch (e: Throwable) {
            throw SignInFailureException(e)
        }

        try {
            getIgnoredUsersAsync().await()
        }
        catch(e: Throwable) {}

        return account!!
    }

    /**
     * はてなスターのサービスを利用するためのキーを取得する
     */
    private fun signInStarAsync() : Deferred<Any> = async {
        require(signedIn()) { "need to sign-in to use the star service" }
        val url = "$S_BASE_URL/entries.json?${cacheAvoidance()}"
        val response = getJson<StarsEntries>(url)

        mRksForStar = response.rks ?: throw RuntimeException("connection error: $S_BASE_URL")
        mSignedInStar = true
    }


    /**
     * ログアウト
     */
    fun signOut() {
        cookieManager.cookieStore.removeAll()
        mRk = null
        mRksForStar = null
        account = null
        ignoredUsers = emptyList()
        mIgnoredUsersLastUpdated = LocalDateTime.MIN
        mSignedIn = false
        mSignedInStar = false
    }

    /**
     * アカウント情報を取得
     */
    fun getAccountAsync() : Deferred<Account> = async {
        // signedIn() = mRk != null && account != null
        if (mRk == null) throw RuntimeException("need to sign-in to get account")

        val url = "$B_BASE_URL/my.name"
        account = getJson<Account>(url)
        return@async account!!
    }

    /**
     * 非表示ユーザーリストを取得（部分的に取得可能）
     */
    fun getIgnoredUsersAsync(limit: Int?, cursor: String?) : Deferred<IgnoredUsersResponse> = async {
        require(signedIn()) { "need to sign-in to get ignored users" }

        val url = buildString {
            append("$B_BASE_URL/api/my/ignore_users?${cacheAvoidance()}")
            if (limit == null) {
                val count = account!!.ignoresRegex.count { it == '|' } * 2
                append("&limit=$count")
            }
            else {
                append("&limit=$limit")
            }
            if (cursor != null) {
                append("&cursor=$cursor")
            }
        }

        val response = getJson<IgnoredUsersResponse>(url)
        ignoredUsers = response.users

        return@async response
    }

    /**
     * 非表示ユーザーリストを取得
     */
    fun getIgnoredUsersAsync(forciblyUpdate: Boolean = false) : Deferred<List<String>> = async {
        if (!signedIn()) return@async emptyList()

        if (forciblyUpdate
            || Duration.between(mIgnoredUsersLastUpdated, LocalDateTime.now()) > mIgnoredUsersUpdateIntervals
        ) {
            try {
                getIgnoredUsersAsync(null, null).await()
                mIgnoredUsersLastUpdated = LocalDateTime.now()
            }
            catch (e: Throwable) {
                throw FetchIgnoredUsersFailureException(e.message)
            }
        }
        return@async ignoredUsers
    }

    /**
     * 対象urlをブックマークする
     */
    fun postBookmarkAsync(
        url: String,
        comment: String = "",
        postTwitter: Boolean = false,
        postFacebook: Boolean = false,
        postEvernote: Boolean = false,
        readLater: Boolean = false,
        isPrivate: Boolean = false
    ) : Deferred<BookmarkResult> = async {

        require(signedIn()) { "need to login for bookmarking" }

        val apiUrl = "$B_BASE_URL/${account!!.name}/add.edit.json?with_status_op=1&from=android-app"
        val params = mapOf(
            "url" to url,
            "comment" to comment,
            "post_twitter" to postTwitter.int.toString(),
            "post_facebook" to postFacebook.int.toString(),
            "post_evernote" to postEvernote.int.toString(),
            "private" to isPrivate.int.toString(),
            "read_later" to readLater.int.toString(),
            "rks" to account!!.rks
        )

        post(apiUrl, params).use { response ->
            if (response.isSuccessful) {
                val gson = GsonBuilder()
                    .serializeNulls()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .registerTypeAdapter(Boolean::class.java, BooleanDeserializer())
                    .create()

                response.body!!.use { body ->
                    val json = body.string()
                    return@async gson.fromJson<BookmarkResult>(json, BookmarkResult::class.java)
                }
            }
        }

        throw RuntimeException("failed to bookmark")
    }

    /**
     * 対象urlのブックマークを削除する
     */
    fun deleteBookmarkAsync(url: String) = async {
        require(signedIn()) { "need to login for deleting bookmarks" }
        val account = account!!
        val apiUrl = "$B_BASE_URL/${account.name}/api.delete_bookmark.json"

        try {
            post(apiUrl, mapOf(
                "url" to url,
                "rks" to account.rks)).close()
        }
        catch (e: IOException) {
            throw RuntimeException(e)
        }
    }


    /**
     * 自分がブクマしたエントリを取得する
     */
    fun getMyBookmarkedEntriesAsync(
        limit: Int? = null,
        of: Int? = null
    ) : Deferred<List<Entry>> = async {

        require(signedIn()) { "need to sign-in to get user's bookmarked entries" }

        val url = buildString {
            append("$B_BASE_URL/api/ipad.mybookmarks?${cacheAvoidance()}")
            if (limit != null) append("&limit=$limit")
            if (of != null) append("&of=$of")
        }

        val listType = object : TypeToken<List<Entry>>() {}.type
        return@async getJson<List<Entry>>(listType, url)
    }

    /**
     * 自分がブクマしたエントリを検索
     */
    fun searchMyEntriesAsync(
        query: String,
        searchType: SearchType,
        limit: Int? = null,
        of: Int? = null) : Deferred<List<Entry>> = async {

        require (signedIn()) { "need to sign-in to search user's bookmarked entries" }

        val url = buildString {
            append("$B_BASE_URL/api/ipad.mysearch/${searchType.name.lowercase()}?${cacheAvoidance()}&q=$query")
            if (limit != null) append("&limit=$limit")
            if (of != null) append("&of=$of")
        }

        val listType = object : TypeToken<List<Entry>>() {}.type
        return@async getJson<List<Entry>>(listType, url)
    }

    /**
     * マイホットエントリを取得
     */
    fun getMyHotEntriesAsync(
        date: String? = null,
        includeAmpUrls: Boolean = true
    ) : Deferred<List<Entry>> = async {

        require(signedIn()) { "need to sign-in to get myhotentries" }

        val url = buildString {
            append("$B_BASE_URL/api/entries/myhotentry.json?${cacheAvoidance()}&include_amp_urls=${includeAmpUrls.int}")
            if (!date.isNullOrEmpty()) {
                append("&date=$date")
            }
        }
        val listType = object : TypeToken<List<Entry>>() {}.type
        return@async getJson<List<Entry>>(listType, url)
    }

    /**
     * カテゴリ情報を取得する
     */
    fun getCategoryEntriesAsync() : Deferred<List<CategoryEntry>> = async {
        val url = "$B_BASE_URL/api/ipad.categories.json?${cacheAvoidance()}"
        val response = getJson<CategoryEntriesResponse>(CategoryEntriesResponse::class.java, url)
        return@async response.categories
    }

    /**
     * 指定カテゴリの特集を取得する
     */
    fun getIssuesAsync(category: Category) : Deferred<List<Issue>> = async {
        val url = "$B_BASE_URL/api/internal/cambridge/category/${category.code}/issues?${cacheAvoidance()}"
        val response = getJson<IssuesResponse>(IssuesResponse::class.java, url)
        return@async response.issues
    }

    /**
     * カテゴリを指定してエントリを取得する
     */
    fun getEntriesAsync(
        entriesType: EntriesType,
        category: Category,
        limit: Int? = null,
        of: Int? = null,
        includeAMPUrls: Boolean = true,
        includeBookmarksOfFollowings: Boolean = true,
        includeBookmarkedData: Boolean = true,
        ad: Boolean = false
    ) : Deferred<List<Entry>> {

        return async {
            val target = when (entriesType) {
                EntriesType.Recent -> "newentry"
                EntriesType.Hot -> "hotentry"
            }

            val url = buildString {
                append(
                    "$B_BASE_URL/api/ipad.$target.json?${cacheAvoidance()}",
                    "&category_id=${category.code}",
                    "&include_amp_urls=${includeAMPUrls.int}",
                    "&include_bookmarked_data=${includeBookmarkedData.int}",
                    "&include_bookmarks_of_followings=${includeBookmarksOfFollowings.int}",
                    "&ad=${ad.int}"
                )
                if (limit != null) append("&limit=$limit")
                if (of != null) append("&of=$of")
            }

            val listType = object : TypeToken<List<Entry>>() {}.type
            return@async getJson<List<Entry>>(listType, url)
        }
    }

    /**
     * 特集を指定してエントリを取得する
     */
    fun getEntriesAsync(
        entriesType: EntriesType,
        issue: Issue,
        limit: Int? = null,
        of: Int? = null,
        includeAMPUrls: Boolean = true,
        includeBookmarksOfFollowings: Boolean = true,
        includeBookmarkedData: Boolean = true,
        includeBookmarksByVisitor: Boolean = true
    ) : Deferred<List<Entry>> {

        return async {
            val target = when (entriesType) {
                EntriesType.Recent -> "newentries"
                EntriesType.Hot -> "hotentries"
            }

            val url = buildString {
                append(
                    "$B_BASE_URL/api/internal/cambridge/issue/${issue.code}/$target?${cacheAvoidance()}",
                    "&include_amp_urls=${includeAMPUrls.int}",
                    "&include_bookmarked_data=${includeBookmarkedData.int}",
                    "&include_bookmarks_by_visitor=${includeBookmarksByVisitor.int}",
                    "&include_bookmarks_of_followings=${includeBookmarksOfFollowings.int}"
                )
                if (limit != null) append("&limit=$limit")
                if (of != null) append("&of=$of")
            }

            val response = getJson<EntriesWithIssue>(EntriesWithIssue::class.java, url)
            return@async response.entries
        }
    }

    /**
     * 指定サイトの既にブクマが付いているエントリ一覧を取得
     */
    fun getEntriesAsync(
        url: String,
        entriesType: EntriesType,
        allMode: Boolean = false,
        page: Int = 1
    ) : Deferred<List<Entry>> = async {

        val sort = when (entriesType) {
            EntriesType.Recent -> if (allMode) "eid" else ""
            EntriesType.Hot -> "count"
        }

        val apiUrl = buildString {
            append(
                "$B_BASE_URL/entrylist?${cacheAvoidance()}&url=${Uri.encode(url)}",
                "&page=$page",
                "&sort=$sort")
        }

        // エントリIDは個別のブクマページを取得しないと分からないので取得タスクをまとめて待機する
//        val entryIdsTasks = ArrayList<Deferred<Long?>>()

        return@async get(apiUrl).use { response ->
            val responseStr = response.body?.use { it.string() } ?: throw RuntimeException("failed to get entries: $url")
            val html = Jsoup.parse(responseStr)

            val anondRootUrl = "https://anond.hatelabo.jp/"
            val anondImageUrl = "https://cdn-ak-scissors.b.st-hatena.com/image/square/abf4f339344e96f39ffb9c18856eca5d454e63f8/height=280;version=1;width=400/https%3A%2F%2Fanond.hatelabo.jp%2Fimages%2Fog-image-1500.gif"

            val countRegex = Regex("""(\d+)\s*users""")
            val thumbnailRegex = Regex("""background-image:url\('(.+)'\);""")
            val rootUrlRegex = Regex("""^/site/""")
            val classNamePrefix = "entrylist-contents"

            val dateTimeFormat = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm")

            html.body().getElementsByClass("$classNamePrefix-main").mapNotNull m@ { entry ->
                val (title, entryUrl) = entry.getElementsByClass("$classNamePrefix-title").firstOrNull()?.let {
                    it.getElementsByTag("a").firstOrNull()?.let { link ->
                        link.attr("title") to link.attr("href")
                    }
                } ?: return@m null

                val count = entry.getElementsByClass("$classNamePrefix-users").firstOrNull()?.let {
                    countRegex.find(it.wholeText())?.groupValues?.get(1)?.toIntOrNull()
                } ?: return@m null

                val rootUrl =
                    entry.getElementsByAttributeValue("data-gtm-click-label", "entry-info-root-url")
                        .firstOrNull()
                        ?.attr("href")
                        ?.let { path -> Uri.decode(rootUrlRegex.replaceFirst(path, "https://")) }
                        ?: entryUrl

                val faviconUrl = entry.getElementsByClass("favicon").firstOrNull()?.attr("src") ?: ""

                val (description, imageUrl) = entry.getElementsByClass("$classNamePrefix-body").firstOrNull()?.let {
                    val description = it.wholeText() ?: ""
                    val imageUrl = it.getElementsByAttributeValue("data-gtm-click-label", "entry-info-thumbnail").firstOrNull()?.attr("style")?.let { style ->
                        thumbnailRegex.find(style)?.groupValues?.get(1)
                    } ?: if (rootUrl == anondRootUrl) anondImageUrl else ""

                    description to imageUrl
                } ?: ("" to "")

                val date = entry.getElementsByClass("$classNamePrefix-date").firstOrNull()?.let {
                    val text = it.wholeText() ?: return@let null
                    try {
                        LocalDateTime.from(dateTimeFormat.parse(text))
                    }
                    catch (e: Throwable) {
                        null
                    }
                }

                Entry(
                    id = 0,  // eidはコメントページを見ないと手に入らない
                    title = title,
                    description = description,
                    count = count,
                    url = entryUrl,
                    rootUrl = rootUrl,
                    faviconUrl = faviconUrl,
                    _imageUrl = imageUrl,
                    date = date
                )
            }
        }
    }

    /**
     * ページのエントリIDを取得する（ブックマークが存在しない場合nullが返る）
     */
    fun getEntryIdAsync(url: String) : Deferred<Long?> = async {
        val bookmarkUrl = getCommentPageUrlFromEntryUrl(url)
        return@async get(bookmarkUrl).use { response ->
            if (response.code != 200) return@use null

            val html = Jsoup.parse(response.body!!.use { it.string() })
            html.getElementsByTag("html")?.firstOrNull()?.attr("data-entry-eid")?.toLongOrNull()
        }
    }

    /**
     * 指定ユーザがブクマしたエントリを取得する
     */
    fun getUserEntriesAsync(
        user: String,
        tag: String? = null,
        limit: Int? = null,
        of: Int? = null
    ) : Deferred<List<Entry>> = async {

        val url = buildString {
            append("$B_BASE_URL/api/internal/user/$user/bookmarks?${cacheAvoidance()}")
            if (limit != null) {
                append("&limit=$limit")
            }
            if (of != null) {
                append("&offset=$of")
            }
            if (tag != null) {
                append("&tag=$tag")
            }
        }

        val response = getJson<UserEntryResponse>(url)
        return@async response.bookmarks.map { it.toEntry() }
    }

    /**
     * 指定ユーザがeidのエントリにつけたブクマのブクマページ情報を取得する
     * （ブクマページURLからEntryを取得するのに使用）
     */
    fun getBookmarkPageAsync(eid: Long, user: String) : Deferred<BookmarkPage> = async {
        val url = "$B_BASE_URL/api/internal/cambridge/entry/$eid/comment/$user"
        val response = getJson<BookmarkPageResponse>(url)
        return@async response.bookmark
    }

    /**
     * エントリ検索
     */
    fun searchEntriesAsync(
        query: String,
        searchType: SearchType,
        entriesType: EntriesType = EntriesType.Recent,
        limit: Int? = null,
        of: Int? = null
    ) : Deferred<List<Entry>> = async {

        val url = buildString {
            append(
                "$B_BASE_URL/api/ipad.search/${searchType.name.lowercase()}?${cacheAvoidance()}",
                "&q=${Uri.encode(query)}",
                "&sort=${entriesType.name.lowercase()}",
                "&include_bookmarked_data=1"
            )
            if (limit != null) append("&limit=$limit")
            if (of != null) append("&of=$of")
        }

        val listType = object : TypeToken<List<Entry>>() {}.type
        return@async getJson<List<Entry>>(listType, url)
    }

    /**
     * ブックマーク情報を取得する
     */
    fun getBookmarksEntryAsync(url: String) : Deferred<BookmarksEntry> = async {
        val apiUrl = "$B_BASE_URL/entry/jsonlite/?url=${Uri.encode(url)}&${cacheAvoidance()}"
        // ブクマが付いていないエントリを取得しようとするとcode=200でnullが返ってくる
        return@async getJson<BookmarksEntry?>(apiUrl, "uuuu/MM/dd HH:mm") ?: BookmarksEntry()
    }

    fun getEmptyEntryAsync(url: String) : Deferred<Entry> = async {
        return@async get(url).use { response ->
            if (response.isSuccessful) {
                val bodyBytes = response.body!!.use { it.bytes() }

                // 文字コードを判別してからHTMLを読む
                val defaultCharsetName = Charset.defaultCharset().name().lowercase()
                var charsetName = defaultCharsetName
                var charsetDetected = false

                val charsetRegex = Regex("""charset=([a-zA-Z0-9_\-]+)""")
                fun parseCharset(src: String): String {
                    val matchResult = charsetRegex.find(src)
                    return if (matchResult?.groups?.size ?: 0 >= 2) matchResult!!.groups[1]!!.value.lowercase()
                    else ""
                }

                // レスポンスヘッダで判断できる場合
                val contentType = response.header("Content-Type")
                if (contentType?.isNotEmpty() == true) {
                    val parsed = parseCharset(contentType)
                    if (parsed.isNotEmpty()) {
                        charsetName = parsed
                        charsetDetected = true
                    }
                }

                val rawHtml = bodyBytes.toString(Charset.defaultCharset())
                val rawDoc = Jsoup.parse(rawHtml)

                if (!charsetDetected) {
                    // HTMLからcharset指定を探す必要がある場合
                    rawDoc.getElementsByTag("meta").let { elem ->
                        // <meta charset="???">
                        val charsetMeta = elem.firstOrNull { it.hasAttr("charset") }
                        if (charsetMeta != null) {
                            charsetName = charsetMeta.attr("charset").lowercase()
                            charsetDetected = true
                            return@let
                        }

                        // <meta http-equiv="Content-Type" content="text/html; charset=???">
                        val meta =
                            elem.firstOrNull { it.attr("http-equiv")?.lowercase() == "content-type" }
                        meta?.attr("content")?.let {
                            val parsed = parseCharset(it)
                            if (parsed.isNotEmpty()) {
                                charsetName = parsed
                                charsetDetected = true
                            }
                        }
                    }
                }
                when (charsetName) {
                    "shift-jis", "shift_jis", "sjis" -> charsetName = "MS932"
                }
                // 文字コード判別ここまで

                val doc =
                    if (charsetName == defaultCharsetName) {
                        rawDoc
                    }
                    else {
                        Jsoup.parse(bodyBytes.inputStream(), charsetName, url)
                    }

                // コメントページの"og:url"には元のエントリページのURLが入っているため，分けて処理する
                val isCommentPage = Regex("""^$B_BASE_URL/entry/\d+/comment/\S+$""").matches(url)

                val allElements = doc.allElements

                val pageTitle =
                    doc.select("title").html().let { title ->
                        if (title.isNullOrEmpty()) url
                        else title
                    }

                val title =
                    if (isCommentPage) pageTitle
                    else allElements.firstOrNull { it.tagName() == "meta" && it.attr("property") == "og:title" }
                        ?.attr("content")
                        ?: pageTitle

                val description =
                    allElements.firstOrNull { it.tagName() == "meta" && it.attr("property") == "og:description" }
                    ?.attr("content")
                    ?: ""

                val actualUrl =
                    if (isCommentPage) url
                    else allElements.firstOrNull { it.tagName() == "meta" && it.attr("property") == "og:url" }
                        ?.attr("content")
                        ?: url

                val imageUrl =
                    allElements.firstOrNull { it.tagName() == "meta" && it.attr("property") == "og:image" }
                    ?.attr("content")
                    ?: ""

                val uri = Uri.parse(actualUrl)
                Entry(
                    id = 0,
                    title = title,
                    description = description,
                    count = 0,
                    url = actualUrl,
                    rootUrl = getTemporaryRootUrl(uri),
                    faviconUrl = getFaviconUrl(uri),
                    _imageUrl = imageUrl)
            }
            else {
                val uri = Uri.parse(url)
                Entry(
                    id = 0,
                    title = "",
                    description = "",
                    count = 0,
                    url = url,
                    rootUrl = getTemporaryRootUrl(uri),
                    faviconUrl = getFaviconUrl(uri),
                    _imageUrl = "")
            }
        }
    }

    /**
     * まだ誰にもブックマークされていないページのダミーブックマーク情報を作成する
     */
    fun getEmptyBookmarksEntryAsync(url: String) : Deferred<BookmarksEntry> = async {
        return@async getEmptyEntryAsync(url).await().let {
            BookmarksEntry(
                id = 0,
                title = it.title,
                bookmarks = emptyList(),
                count = 0,
                url = it.url,
                entryUrl = it.url,
                screenshot = it.imageUrl
            )
        }
    }

    /**c
     * エントリIDからエントリ情報を取得する
     * 失敗時例外送出: RuntimeException()  ; TODO: 例外型なんとかしたい
     */
    fun getEntryAsync(eid: Long) : Deferred<Entry> {
        val url = "$B_BASE_URL/entry/$eid"
        return getEntryImplAsync(url)
    }

    /**
     * エントリが存在するかどうかを調べ、存在する場合はエントリ情報を返す。
     * 存在しない場合は疑似的な内容のEntryを作成して返す
     */
    fun getEntryAsync(url: String) : Deferred<Entry> = async {
        try {
            val commentPageUrl = getCommentPageUrlFromEntryUrl(url)
            getEntryImplAsync(commentPageUrl).await()
        }
        catch (e: Throwable) {
            getEmptyEntryAsync(url).await()
        }
    }

    /**
     * コメントページのURLを渡してエントリが存在するかどうかを調べ、存在する場合はエントリ情報を返す。
     * 存在しない場合は例外を送出する
     */
    private fun getEntryImplAsync(commentPageUrl: String) : Deferred<Entry> = async {
        return@async get(commentPageUrl).use { response ->
            if (!response.isSuccessful) {
                when (response.code) {
                    404 -> throw NotFoundException()
                    else -> throw RuntimeException("cannot get an entry: $commentPageUrl")
                }
            }

            val bodyBytes = response.body!!.use { it.bytes() }
            val bodyStr = bodyBytes.toString(Charsets.UTF_8)
            val doc = Jsoup.parse(bodyStr)
            val root = doc.getElementsByTag("html").first()

            val eid = root.attr("data-entry-eid").toLong()
            val count = root.attr("data-bookmark-count").toInt()
            val entryUrl = root.attr("data-entry-url")

            val imageUrl = doc.head().getElementsByTag("meta")
                .firstOrNull { it.attr("property") == "og:image" || it.attr("name") == "twitter:image:src" }
                ?.attr("content")
                ?: ""

            val title = doc.getElementsByClass("entry-info-title").firstOrNull()?.text() ?: entryUrl

            val domainElement = doc.getElementsByAttributeValue("data-gtm-label", "entry-info-domain").firstOrNull()
            val rootUrl = domainElement?.text() ?: getTemporaryRootUrl(entryUrl)
            val faviconUrl = domainElement?.getElementsByTag("img")?.firstOrNull()?.attr("src") ?: getFaviconUrl(entryUrl)

            val description = doc.getElementsByClass("entry-about-description").firstOrNull()?.text() ?: ""

            // サインインユーザーのブクマ情報を埋め込む
            val account = account
            val bookmarkedData =
                if (account == null) null
                else {
                    val userBookmarkPageResult = runCatching {
                        getBookmarkPageAsync(eid, account.name).await()
                    }

                    userBookmarkPageResult.getOrNull()?.let { page ->
                        BookmarkResult(
                            user = page.user,
                            comment = page.comment.body,
                            tags = page.comment.tags,
                            timestamp = page.timestamp,
                            userIconUrl = getUserIconUrl(page.user),
                            commentRaw = page.comment.raw,
                            permalink = page.permalink,
                            success = true,
                            private = page.status == "private",
                            eid = eid
                        )
                    }
                }

            Entry(
                id = eid,
                title = title,
                description = description,
                count = count,
                url = entryUrl,
                rootUrl = rootUrl,
                faviconUrl = faviconUrl,
                _imageUrl = imageUrl,
                bookmarkedData = bookmarkedData
            )
        }
    }

    /**
     * ブックマークリストを取得する
     */
    fun getRecentBookmarksAsync(
        url: String,
        limit: Long? = null,
        cursor: String? = null
    ) : Deferred<BookmarksWithCursor> = async {
        val apiUrl = buildString {
            append("$B_BASE_URL/api/ipad.entry_bookmarks_with_cursor?${cacheAvoidance()}&url=${Uri.encode(url)}")
            if (limit != null) append("&limit=$limit")
            if (cursor != null) append("&cursor=$cursor")
        }
        return@async try {
            getJson<BookmarksWithCursor>(BookmarksWithCursor::class.java, apiUrl, withCookie = false)
        }
        catch (e: NotFoundException) {
            // まだブクマされていない場合
            BookmarksWithCursor()
        }
    }

    /**
     * ブックマーク概要を取得する
     */
    fun getDigestBookmarksAsync(url: String, limit: Long? = null) : Deferred<BookmarksDigest> = async {
        val apiUrl = buildString {
            append("$B_BASE_URL/api/ipad.entry_reactions?${cacheAvoidance()}&url=${Uri.encode(url)}")
            if (limit != null) append("&limit=$limit")
        }
        return@async try {
            if (signedIn()) {
                val tasks = listOf<Deferred<BookmarksDigest>>(
                    async { getJson(apiUrl, withCookie = false) },
                    async { getJson(apiUrl, withCookie = true) }
                )
                tasks.awaitAll()
                tasks[0].await().copy(
                    favoriteBookmarks = tasks[1].await().favoriteBookmarks
                )
            }
            else {
                getJson<BookmarksDigest>(apiUrl, withCookie = true)
            }
        }
        catch (e: NotFoundException) {
            // まだブクマされていない場合
            BookmarksDigest()
        }
    }

    /**
     * エントリーIDから元の記事のURLを取得する
     */
    fun getEntryUrlFromIdAsync(eid: Long) : Deferred<String> = async {
        val url = "$B_BASE_URL/entry/$eid"
        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)

        call.execute().use { response ->
            if (response.isSuccessful) {
                val commentPageUrl = response.request.url.toString()

                val headHttps = "$B_BASE_URL/entry/s/"
                val head = "$B_BASE_URL/entry/"

                val isHttps = commentPageUrl.startsWith(headHttps)
                val tail = commentPageUrl.substring(if (isHttps) headHttps.length else head.length)
                return@async "${if (isHttps) "https" else "http"}://$tail"
            }
        }

        throw RuntimeException("connection error")
    }

    /**
     * エントリーIDからブックマーク情報を取得する
     */
    fun getBookmarksEntryAsync(eid: Long) : Deferred<BookmarksEntry> = async {
        val url = getEntryUrlFromIdAsync(eid).await()
        return@async getBookmarksEntryAsync(url).await()
    }

    /**
     * ブックマーク概要を取得する
     */
    fun getDigestBookmarksAsync(eid: Long, limit: Long? = null) : Deferred<BookmarksDigest> = async {
        val url = getEntryUrlFromIdAsync(eid).await()
        return@async getDigestBookmarksAsync(url, limit).await()
    }

    /**
     * ユーザーをお気に入りに追加する
     */
    suspend fun follow(user: String) = withContext(Dispatchers.IO) {
        require (signedIn()) { "need to sign-in to follow an user" }
        val userSignedIn = account!!.name
        val url = "$B_BASE_URL/$userSignedIn/api.follow.json"
        val params = mapOf(
            "username" to user,
            "rks" to account!!.rks
        )
        post(url, params)
    }

    /**
     * ユーザーのお気に入りを解除する
     */
    suspend fun unfollow(user: String) = withContext(Dispatchers.IO) {
        require (signedIn()) { "need to sign-in to unfollow an user" }
        val userSignedIn = account!!.name
        val url = "$B_BASE_URL/$userSignedIn/api.unfollow.json"
        val params = mapOf(
            "username" to user,
            "rks" to account!!.rks
        )
        post(url, params)
    }

    /**
     * お気に入りユーザーの一覧を取得する
     */
    fun getFollowingsAsync() : Deferred<List<String>> {
        require (signedIn()) { "need to sign-in to get following users" }
        return getFollowingsAsync(account!!.name)
    }

    /**
     * 指定ユーザーのお気に入りリストを取得する
     */
    fun getFollowingsAsync(user: String) : Deferred<List<String>> = async {
        val url = "$B_BASE_URL/$user/follow.json?${cacheAvoidance()}"
        val response = getJson<FollowingsResponse>(url)
        return@async response.followings.map { it.name }
    }

    /**
     * フォロワーリストを取得する
     */
    fun getFollowersAsync() : Deferred<List<Follower>> {
        require (signedIn()) { "need to sign-in to get followers" }
        return getFollowersAsync(account!!.name)
    }

    /**
     * 指定ユーザーのフォロワーリストを取得する
     */
    fun getFollowersAsync(user: String) : Deferred<List<Follower>> = async {
        val url = "$B_BASE_URL/api/internal/cambridge/user/$user/followers?${cacheAvoidance()}"
        val response = getJson<FollowersResponse>(url)
        return@async response.followers
    }

    /**
     * お気に入りユーザーの最近のブクマを取得する
     */
    fun getFollowingBookmarksAsync(
        includeAmpUrls: Boolean = true,
        limit: Int? = null,
        offset: Int? = null
    ) : Deferred<List<FollowingBookmark>> = async {
        require (signedIn()) { "need to sign-in to get bookmarks of followings" }
        val url = buildString {
            append("$B_BASE_URL/api/internal/cambridge/user/my/feed/following/bookmarks?")
            append(cacheAvoidance())
            append("&include_amp_urls=${includeAmpUrls.int}")
            limit?.let { append("&limit=$it") }
            offset?.let { append("&of=$it") }
        }
        val response = getJson<FollowingBookmarksResponse>(url)
        return@async response.bookmarks
    }

    /**
     * スター情報を複数一括で取得する
     */
    fun getStarsEntryAsync(urls: Iterable<String>) : Deferred<List<StarsEntry>> = async {
        val apiBaseUrl = "$S_BASE_URL/entry.json?${cacheAvoidance()}&"
        val windowSize = 50
        val windows = urls.windowed(size = windowSize, step = windowSize, partialWindows = true)
        val tasks = windows.map { urls ->
            async {
                val params = urls.joinToString(separator = "&") { url -> "uri=${Uri.encode(url)}" }
                getJson<StarsEntries>(apiBaseUrl + params)
            }
        }

        return@async tasks.awaitAll()
            .flatMap { it.entries }
    }

    /**
     * スター情報を取得する
     */
    fun getStarsEntryAsync(url: String) : Deferred<StarsEntry> = async {
        val apiUrl = "$S_BASE_URL/entry.json?${cacheAvoidance()}&uri=${Uri.encode(url)}"
        val response = getJson<StarsEntries>(apiUrl)
        return@async response.entries.getOrNull(0) ?: StarsEntry(url = url, stars = emptyList(), coloredStars = null)
    }

    /** はてなスターへのサインインが済んでいるかを確認。必要かつ可能ならばここで再度ログインを試みる */
    private suspend fun checkSignedInStar(message: String? = null) {
        if (!signedInStar()) {
            if (signedIn()) {  // TODO: b.hatenaへの再サインインを試行する
                try {
                    signInStarAsync().await()
                }
                catch (e: Throwable) {
                    throw SignInStarFailureException(message, e)
                }
            }
            else {
                throw SignInStarFailureException("need to sign-in to sign-in s.hatena")
            }
        }
    }

    /**
     * 最近自分が付けたスターを取得する
     */
    fun getRecentStarsAsync() : Deferred<List<StarsEntry>> = async {
        checkSignedInStar("need to sign-in to get my stars")
        val apiUrl = "$S_BASE_URL/${account!!.name}/stars.json?${cacheAvoidance()}"
        val listType = object : TypeToken<List<StarsEntry>>() {}.type
        return@async getJson<List<StarsEntry>>(listType, apiUrl)
    }

    /**
     * 最近自分に付けられたスターを取得する
     */
    fun getRecentStarsReportAsync() : Deferred<List<StarsEntry>> = async {
        checkSignedInStar("need to sign-in to get stars report")
        val apiUrl = "$S_BASE_URL/${account!!.name}/report.json?${cacheAvoidance()}"
        val response = getJson<StarsEntries>(apiUrl)
        return@async response.entries
    }

    private fun changeStarColorPalette(url: String, color: StarColor) : Boolean {
        return try {
            val apiUrl = "$S_BASE_URL/colorpalette.json"
            val palette = getJson<StarPalette>(
                apiUrl +
                        "?uri=${Uri.encode(url)}" +
                        "&date=${LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)}"
            )

            if (color == StarColor.Yellow) {
                return true
            }
            else if (!palette.colorStarCounts.has(color)) {
                return false
            }

            val data = mapOf(
                "uri" to url,
                "color" to color.name.lowercase(),
                "token" to palette.token
            )

            val response = post(apiUrl, data)

            response.isSuccessful
        }
        catch (e: Throwable) {
            false
        }
    }

    /**
     * 対象URLにスターをつける
     */
    @Throws(
        SignInStarFailureException::class,
        ConnectionFailureException::class,
        NotFoundException::class,
        SocketTimeoutException::class
    )
    fun postStarAsync(
        url: String,
        color: StarColor = StarColor.Yellow,
        quote: String = ""
    ) : Deferred<Star> = async {
        checkSignedInStar("need to sign-in to post star")

        val paletteChanged = changeStarColorPalette(url, color)
        if (!paletteChanged) {
            throw ConnectionFailureException("failed to change the star palette")
        }

        val apiUrl = "$S_BASE_URL/star.add.json?${cacheAvoidance()}" +
                "&uri=${Uri.encode(url)}" +
                "&rks=$mRksForStar" +
                "&quote=${Uri.encode(quote)}"

        return@async getJson<Star>(apiUrl)
    }

    /**
     * 一度付けたスターを削除する
     */
    @Throws(
        SignInStarFailureException::class,
        ConnectionFailureException::class,
        NotFoundException::class,
        SocketTimeoutException::class
    )
    fun deleteStarAsync(url: String, star: Star) : Deferred<Any> = async {
        checkSignedInStar("need to sign-in to delete star")
        val rkm = deleteStarConfirm(url, star)
        val params = mapOf(
            "uri" to url,
            "rkm" to rkm,
            "rks" to mRksForStar!!,
            "name" to star.user,
            "color" to star.color.name.lowercase(),
            "quote" to star.quote,
            "only" to "content",
            "delete_star" to "on"
        )
        post("$S_BASE_URL/star.delete", params).use { response ->
            if (!response.isSuccessful) throw ConnectionFailureException("failed to delete a star")
        }
    }

    private suspend fun deleteStarConfirm(url: String, star: Star) : String {
        val apiUrl = buildString {
            append("$S_BASE_URL/star.deleteconfirm?")
            append("color=${star.color}")
            append("&name=${star.user}")
            append("&uri=${Uri.encode(url)}")
            append("&only=content")
        }
        get(apiUrl).use { response ->
            if (!response.isSuccessful) throw ConnectionFailureException("failed to delete a star")
            val doc = Jsoup.parse(response.body!!.byteStream(), "UTF-8", S_BASE_URL)
            val rkmInputTag = doc.getElementsByAttributeValue("name", "rkm").first()
            return rkmInputTag.attr("value")
        }
    }

    /**
     *  ログインユーザーが持っているカラースターの情報を取得する
     */
    fun getMyColorStarsAsync() : Deferred<UserColorStarsCount> = async {
        checkSignedInStar("need to sign-in to get color stars")

        val url = "$S_BASE_URL/api/v0/me/colorstars?${cacheAvoidance()}"

        @Suppress("SpellCheckingInspection")
        val request = Request.Builder()
            .get()
            .url(url)
            .header("Content-Type", "application/json")
            .header("X-Internal-API-Key", "wvlYJXSGDMY161Bbw4TEf8unWl4pDLLB1gy7PGcA")
            .header("X-Internal-API-RK", mRk!!.value)
            .build()

        val response = send<UserColorStarsResponse>(request)
        if (response.success) {
            return@async response.result["counts"] ?: UserColorStarsCount(0, 0, 0, 0)
        }

        throw FetchUserStarsException()
    }

    /**
     * 通知を取得する
     */
    fun getNoticesAsync() : Deferred<NoticeResponse> = async {
        require(signedIn()) { "need to sign-in to get user's notices" }
        val url = "$W_BASE_URL/notify/api/pull?${cacheAvoidance()}"
        val response = getJson<NoticeResponse>(url)
        // コメント情報がない通知のブコメを取得する
        val fixNoticesTasks = response.notices.map { notice ->
            async {
                runCatching {
                    if (notice.verb == Notice.VERB_STAR && notice.metadata?.subjectTitle.isNullOrBlank()) {
                        val md = NoticeMetadata(
                            getBookmarkPageAsync(
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

        return@async response.copy(notices = fixedNotices)
    }

    /**
     * 通知既読状態を更新する
     */
    fun updateNoticesLastSeenAsync() : Deferred<Any> = async {
        require(signedIn()) { "need to sign-in to update notices last seen" }
        val url = "$W_BASE_URL/notify/api/read"

        try {
            post(url, mapOf("rks" to account!!.rks)).use { response ->
                if (!response.isSuccessful) {
                    throw RuntimeException("failed to update notices last seen")
                }
            }
        }
        catch (e: IOException) {
            throw RuntimeException("failed to update notices last seen")
        }
    }

    /**
     * 指定ユーザーが使用しているタグを取得する
     */
    fun getUserTagsAsync(user: String) : Deferred<List<Tag>> = async {
        val url = "$B_BASE_URL/$user/tags.json?${cacheAvoidance()}"
        val response = getJson<TagsResponse>(url)
        if (response.status != 200) throw RuntimeException("failed to get $user's tags: ${response.status}")
        return@async response.tags
            .map { Tag(it.key, it.value.index, it.value.count, it.value.timestamp) }
            .sortedBy { it.index }
            .toList()
    }

    /**
     * 指定したユーザーのブコメを非表示にする
     */
    fun ignoreUserAsync(user: String) : Deferred<Unit> = async {
        require(signedIn()) { "need to sign-in to mute users" }
        val url = "$B_BASE_URL/${account!!.name}/api.ignore.json"
        try {
            val params = mapOf(
                "username" to user,
                "rks" to account!!.rks
            )
            post(url, params).use { response ->
                if (!response.isSuccessful) throw RuntimeException("failed to mute an user: $user")
                if (!ignoredUsers.contains(user)) {
                    ignoredUsers = ignoredUsers.plus(user)
                }
            }
        }
        catch (e: IOException) {
            throw RuntimeException("failed to mute an user: $user")
        }
    }

    /**
     * 指定したユーザーのブコメ非表示を解除する
     */
    fun unignoreUserAsync(user: String) : Deferred<Unit> = async {
        require(signedIn()) { "need to sign-in to mute users" }
        val url = "$B_BASE_URL/${account!!.name}/api.unignore.json"
        try {
            val params = mapOf(
                "username" to user,
                "rks" to account!!.rks
            )
            post(url, params).use { response ->
                if (!response.isSuccessful) throw RuntimeException("failed to mute an user: $user")
                ignoredUsers = ignoredUsers.filterNot { it == user }.toList()
            }
        }
        catch (e: IOException) {
            throw RuntimeException("failed to mute an user: $user")
        }
    }

    /**
     * ログインユーザーが使用しているタグを取得する
     */
    fun getUserTagsAsync() : Deferred<List<Tag>> {
        require(signedIn()) { "need to sign-in to get user's tags" }
        return getUserTagsAsync(account!!.name)
    }

    /**
     * 暫定的なrootUrlを生成する
     */
    fun getTemporaryRootUrl(url: String) : String = getTemporaryRootUrl(Uri.parse(url))

    /**
     * 暫定的なrootUrlを生成する
     */
    fun getTemporaryRootUrl(uri: Uri) : String = uri.let { it.scheme!! + "://" + it.host!! }

    /**
     * URLからファビコンURLを取得する
     */
    fun getFaviconUrl(url: String) : String = getFaviconUrl(Uri.parse(url))

    /**
     * URLからファビコンURLを取得する
     */
    fun getFaviconUrl(uri: Uri) : String = "https://www.google.com/s2/favicons?domain=${uri.host}"

    /**
     * ユーザー名からアイコンURLを取得する
     */
    fun getUserIconUrl(user: String) : String = "https://cdn1.www.st-hatena.com/users/$user/profile.gif"

    /**
     * URLがブコメページのURLかを判別する
     */
    fun isUrlCommentPages(url: String) : Boolean = url.startsWith("$B_BASE_URL/entry")

    /**
     * ブコメページのURLからエントリのURLを取得する
     * e.g.)
     * 1) https://b.hatena.ne.jp/entry/s/www.hoge.com/ ==> https://www.hoge.com/
     * 2) https://b.hatena.ne.jp/entry/https://www.hoge.com/ ==> https://www.hoge.com/
     * 3) https://b.hatena.ne.jp/entry/{eid}/comment/{username} ==> https://b.hatena.ne.jp/entry/{eid}  (modifySpecificUrls()を参照)
     * 4) https://b.hatena.ne.jp/entry?url=https~~~
     * 5) https://b.hatena.ne.jp/entry?eid=1234
     * 6) https://b.hatena.ne.jp/entry/{eid}
     * 7) https://b.hatena.ne.jp/entry.touch/s/~~~
     * 8) https://b.hatena.ne.jp/entry/panel/?url=~~~
     */
    fun getEntryUrlFromCommentPageUrl(url: String) : String {
        if (url.startsWith("$B_BASE_URL/entry?url=") || url.startsWith("$B_BASE_URL/entry/panel/?url=")) {
            // 4, 8)
            return Uri.parse(url).getQueryParameter("url") ?: throw RuntimeException("invalid comment page url: $url")
        }
        else if (url.startsWith("$B_BASE_URL/entry?eid=")) {
            // 5)
            val eid = Uri.parse(url).getQueryParameter("eid") ?: throw RuntimeException("invalid comment page url: $url")
            return "$B_BASE_URL/entry/$eid"
        }
        else {
            val commentUrlRegex = Regex("""https?://b\.hatena\.ne\.jp/entry/(\d+)(/comment/\w+)?""")
            val commentUrlMatch = commentUrlRegex.matchEntire(url)
            if (commentUrlMatch != null) {
                // 3, 6)
                return "$B_BASE_URL/entry/${commentUrlMatch.groups[1]!!.value}"
            }
            else {
                val regex = Regex("""https?://b\.hatena\.ne\.jp/entry(\.touch)?/(https://|s/)?(.+)""")
                val matches =
                    regex.matchEntire(url)
                        ?: throw RuntimeException("invalid comment page url: $url")

                val path =
                    matches.groups[3]?.value
                        ?: throw RuntimeException("invalid comment page url: $url")

                return if (matches.groups[2]?.value.isNullOrEmpty()) {
                    if (path.startsWith("http://")) {
                        // 2)
                        path
                    }
                    else {
                        // 1)
                        "http://$path"
                    }
                }
                else {
                    // 1,2)
                    "https://$path"
                }
            }
        }
    }

    /**
     * エントリのURLからブコメページのURLを取得する
     * e.g.)  https://www.hoge.com/ ===> https://b.hatena.ne.jp/entry/s/www.hoge.com/
     */
    fun getCommentPageUrlFromEntryUrl(url: String) =
//        "$B_BASE_URL/entry?url=${Uri.encode(url)}"
        buildString {
            append("$B_BASE_URL/entry/")
            append(
                when {
                    url.startsWith("https://") -> "s/${url.substring("https://".length)}"
                    url.startsWith("http://") -> url.substring("http://".length)
                    else -> throw RuntimeException("invalid url: $url")
                }
            )
        }


    /**
     * ブコメページのURL
     *
     * TODO: `CommentPage`(ブコメ一覧ページ), `BookmarkComment`(ブコメ)が紛らわしい
     */
    fun getBookmarkCommentUrl(eid: Long, user: String) : String {
        return "$B_BASE_URL/entry/$eid/comment/$user"
    }

    /**
     * 障害情報を取得する
     */
    fun getMaintenanceEntriesAsync() : Deferred<List<MaintenanceEntry>> = async {
        val url = "https://maintenance.hatena.ne.jp"
        try {
            get(url).use { response ->
                require(response.isSuccessful) { "connection failure" }

                val brRegex = Regex("""<br/?>""")
                val dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss")

                fun getTimestamp(header: Element, className: String) : LocalDateTime? =
                    header.getElementsByClass(className).firstOrNull()?.let {
                        val timestampString = it.getElementsByTag("time").firstOrNull()?.wholeText() ?: return null
                        LocalDateTime.parse(timestampString, dateTimeFormatter)
                    }

                val body = response.body!!.use { it.string() }
                val documentRoot = Jsoup.parse(body)

                return@async documentRoot.getElementsByTag("article").mapNotNull { article ->
                    val titleTag = article.getElementsByTag("h2").firstOrNull() ?: return@mapNotNull null
                    val titleLinkTag = titleTag.getElementsByTag("a").firstOrNull() ?: return@mapNotNull null
                    val link = titleLinkTag.attr("href")
                    val title = titleTag.wholeText()
                    val resolved = title.contains("復旧済")
                    val id = titleTag.id()
                    if (id.isNullOrBlank()) return@mapNotNull null

                    val paragraphs = article.getElementsByTag("p")

                    val header = paragraphs.firstOrNull { it.hasClass("sectionheader") } ?: return@mapNotNull null
                    val timestamp = getTimestamp(header, "timestamp") ?: return@mapNotNull null
                    val timestampUpdated = getTimestamp(header, "timestamp updated") ?: timestamp

                    val paragraph = paragraphs.firstOrNull { !it.hasClass("sectionheader") } ?: return@mapNotNull null

                    return@mapNotNull MaintenanceEntry(
                        id = id,
                        title = title,
                        body = paragraph.html().replace(brRegex, "\n"),
                        resolved = resolved,
                        url = url + link,
                        timestamp = timestamp,
                        timestampUpdated = timestampUpdated
                    )
                }
            }
        }
        catch (e: Throwable) {
            throw RuntimeException("failed to get maintenance entries: ${e.message ?: ""}")
        }
    }

    /**
     * ユーザーを通報する(b.hatena)
     */
    fun reportAsync(
        entry: Entry,
        bookmark: Bookmark,
        category: ReportCategory,
        text: String = ""
    ) : Deferred<Boolean> = async {
        require(signedIn()) { "need to sign-in for reporting user" }
        val url = "$B_BASE_URL/-/report/bookmark"
        val params = mapOf(
            "rks" to account!!.rks,
            "url" to entry.url,
            "user_name" to bookmark.user,
            "category" to category.name.lowercase(),
            "text" to text
        )

        post(url, params).use { response ->
            return@async response.isSuccessful
        }
    }

    /**
     * ユーザーを通報する(b.hatena)
     */
    fun reportAsync(report: Report) =
        reportAsync(
            entry = report.entry,
            bookmark = report.bookmark,
            category = report.category,
            text = report.comment ?: ""
        )

    /**
     * ユーザーを通報する(www.hatena)
     */
    fun reportAsync(
        user: String,
        category: ReportCategory,
        text: String = ""
    ) : Deferred<Boolean> = async {
        val url = "$W_BASE_URL/faq/report"
        val location = "$B_BASE_URL/${user}/"
        val params = mapOf(
            "location" to location,
            "referer" to location,
            "c" to "190190366613968214",
            "target_url" to location,
            "target_label" to "id:$user",
            "object_local_id" to "",
            "object_data_category" to "",
            "object_hash" to "",
            "object_permalink_url" to "",
            "report_type" to category.type,
            "content" to text
        )

        post(url, params).use { response ->
            return@async response.isSuccessful
        }
    }

    /**
     * ユーザー情報を取得する
     */
    fun getProfileAsync(user: String) : Deferred<Profile?> = async {
        get("https://profile.hatena.ne.jp/${user}/").use { response ->
            if (!response.isSuccessful) return@async null
            val bookmarkInfoTask = getUserBookmarkInfoAsync(user)

            val html = Jsoup.parse(response.body!!.use { it.string() })
            val spaceRegex = Regex("""\s+""")

            // 基本情報
            val (iconUrl, description) = html.getElementById("user-header-body").let { header ->
                val iconUrl = header.getElementsByClass("userimg").firstOrNull()?.attr("src") ?: ""
                val description = header.getElementsByClass("info").firstOrNull()?.wholeText() ?: ""
                iconUrl to spaceRegex.replace(description, "")
            }

            // プロフィール項目
            val profileKeys = html.getElementsByClass("profile-dt").map {
                spaceRegex.replace(it.wholeText(), "")
            }
            val profileValues = html.getElementsByClass("profile-dd").map {
                spaceRegex.replace(it.wholeText(), "")
            }
            val profiles = profileKeys.zip(profileValues)

            val displayName = profiles.firstOrNull { it.first == "ニックネーム" }?.second ?: user

            // アドレスリスト
            val addresses = html.getElementsByClass("profile addresslist").firstOrNull()?.let { item ->
                val addressKeys = item.getElementsByTag("th").map {
                    spaceRegex.replace(it.wholeText(), "")
                }
                val addressValues = item.getElementsByTag("td").mapNotNull m@ {
                    Profile.Address(
                        text = spaceRegex.replace(it.wholeText(), ""),
                        url =  it.getElementsByTag("a").firstOrNull()?.attr("href") ?: return@m null
                    )
                }
                addressKeys.zip(addressValues)
            } ?: emptyList()

            // 使用中のサービス一覧
            val servicesUl = html.getElementsByClass("hatena-fotolife floatlist").first()
            val services = servicesUl.getElementsByTag("li").mapNotNull m@ { service ->
                val image = service.getElementsByClass("profile-image").firstOrNull() ?: return@m null
                val name = image.attr("title")
                val imageUrl = image.attr("src")
                val url = service.getElementsByTag("a").firstOrNull()?.attr("href") ?: return@m null

                Profile.Service(
                    name = name,
                    url = url,
                    imageUrl = imageUrl
                )
            }

            return@async Profile(
                id = user,
                name = displayName,
                iconUrl = iconUrl,
                description = description,
                profiles = profiles,
                services = services,
                addresses = addresses,
                bookmark = bookmarkInfoTask.await()
            )
        }
    }

    private fun getUserBookmarkInfoAsync(user: String) : Deferred<Profile.Bookmark> = async {
        try {
            get("$B_BASE_URL/$user/").use { response ->
                if (!response.isSuccessful) return@async Profile.Bookmark()

                val html = Jsoup.parse(response.body!!.use { it.string() })
                val dataAttr = "data-gtm-click-label"
                val userAttr = if (signedIn()) "user-my" else "user"

                val statusClass = "userprofile-status-count"
                fun statusCountToInt(doc: Document, className: String) =
                    doc.getElementsByClass(className)
                        .firstOrNull()
                        ?.wholeText()
                        ?.replace(",", "")
                        ?.toInt()
                        ?: 0
                val count = statusCountToInt(html, statusClass)
                val followings = 0//statusCountToInt(html, "$statusClass js-total-followings")
                val followers = 0//statusCountToInt(html, "$statusClass js-total-followers")

                // followings, followersはJSで後から挿入されるっぽい

                val tagCountRegex = Regex("""\((\d+)\)""")
                val allTags =
                    html.getElementsByAttributeValue(dataAttr, "$userAttr-tags").map {
                        val name = it.ownText()
                        val countText =
                            it.getElementsByClass("count").firstOrNull()?.wholeText() ?: "(0)"
                        val tagCount =
                            tagCountRegex.find(countText)?.groupValues?.getOrNull(1)?.toInt() ?: 0
                        Profile.Bookmark.Tag(name, tagCount)
                    }

                val usersCountRegex = Regex("""(\d+)\s*users""")
                val rootUrlRegex = Regex("""\S+""")
                val eidRegex = Regex("""bookmark-(\d+)""")
                val article = "centerarticle"
                val dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu/MM/dd")
                val entries = html.getElementsByClass("bookmark-item js-user-bookmark-item").mapNotNull m@ { item ->
                    val titleArea = item.getElementsByAttributeValue(dataAttr, "$userAttr-bookmark-title").firstOrNull() ?: return@m null
                    val title = titleArea.wholeText()
                    val faviconUrl = titleArea.getElementsByClass("$article-entry-favicon").firstOrNull()?.attr("src") ?: ""
                    val entryUrl = titleArea.attr("href")

                    val bookmarkCount =
                        usersCountRegex.find(
                            item.getElementsByAttributeValue(dataAttr, "$userAttr-bookmark-users").firstOrNull()?.wholeText() ?: "0 users")
                            ?.groupValues
                            ?.getOrNull(1)
                            ?.toInt()
                            ?: 0

                    val rootUrl = rootUrlRegex.find(item.getElementsByAttributeValue(dataAttr, "$userAttr-bookmark-domain").firstOrNull()?.wholeText() ?: "")?.value

                    val (description, imageUrl) = item.getElementsByClass("$article-entry-contents").firstOrNull()?.let {
                        val description = it.getElementsByClass("$article-entry-summary").firstOrNull()?.wholeText() ?: ""
                        val imageUrl = it.getElementsByTag("img").firstOrNull()?.attr("src") ?: ""
                        description to imageUrl
                    } ?: "" to ""

                    // ユーザーがつけたブクマ情報
                    val (eid, reaction) =
                        item.getElementsByClass("$article-reaction js-user-bookmark-id-container").firstOrNull()?.let { reactionArea ->
                            val eid = eidRegex.find(reactionArea.id())?.groupValues?.getOrNull(1)?.toLong() ?: 0L

                            val reactionMain = reactionArea.getElementsByClass("$article-reaction-main").firstOrNull() ?: return@let eid to null
//                            val userName = reactionMain.getElementsByClass("$article-reaction-username").firstOrNull()?.wholeText() ?: user
                            val tags = reactionMain.getElementsByAttributeValue(dataAttr, "$userAttr-reaction-tag").map { tag ->
                                tag.wholeText()
                            }
                            val comment = reactionMain.getElementsByClass("js-comment").firstOrNull()?.wholeText() ?: ""
                            val timestampText = reactionMain.getElementsByClass("$article-reaction-timestamp").first().wholeText()

                            // スター情報はJSで後から挿入されるようなのでhtmlから取得できない

                            val bookmarkedData = BookmarkResult(
                                user = user,
                                comment = comment,
                                tags = tags,
                                timestamp = LocalDate.parse(timestampText, dateTimeFormatter).atStartOfDay(),
                                userIconUrl = getUserIconUrl(user),
                                commentRaw = tags.joinToString("") { "[$it]" } + comment,
                                permalink = "$B_BASE_URL/entry/$eid/comment/$user",
                                eid = eid
                            )

                            eid to bookmarkedData
                        } ?: 0L to null

                    Entry(
                        id = eid,
                        title = title,
                        description = description,
                        count = bookmarkCount,
                        url = entryUrl,
                        rootUrl = rootUrl,
                        faviconUrl = faviconUrl,
                        _imageUrl = imageUrl,
                        bookmarkedData = reaction
                    )
                }

                return@async Profile.Bookmark(
                    count = count,
                    followingCount = followings,
                    followerCount = followers,
                    tags = allTags,
                    entries = entries
                )
            }
        }
        catch (e: Throwable) {
            return@async Profile.Bookmark()
        }
    }

    /** 指定URLのブクマ件数を取得する */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getBookmarkCountsAsync(urls: List<String>) : Deferred<Map<String, Int>> = async {
        val tasks = ArrayList<Deferred<Map<String, Int>?>>()
        val gson = GsonBuilder().create()
        val mapType = object : TypeToken<Map<String, Int>>(){}.type

        // 一度の取得で50個までURLを指定できるので、それを超える場合は分けて実行する
        val windowSize = 50
        urls.windowed(size = windowSize, step = windowSize, partialWindows = true)
            .forEach {
                tasks.add(getBookmarkCountsImplAsync(it, gson, mapType))
            }

        tasks.awaitAll()
        val result = HashMap<String, Int>()
        tasks.mapNotNull { it.getCompleted() }
            .forEach { result.putAll(it) }

        return@async result
    }

    /** 指定URLのブクマ件数を取得する(単数バージョン) */
    fun getBookmarkCountsAsync(url: String) : Deferred<Int> = async {
        val result = getBookmarkCountsAsync(listOf(url)).await()
        return@async result[url] ?: 0
    }

    /** 前処理を行ったURLリストの各ブクマ件数を取得するタスク本体 */
    private fun getBookmarkCountsImplAsync(
        urls: List<String>,
        gson: Gson,
        mapType: Type
    ) : Deferred<Map<String, Int>> {
        val url = urls.joinToString(
            separator = "&url=",
            prefix = "https://bookmark.hatenaapis.com/count/entries?${cacheAvoidance()}&url="
        ) { Uri.encode(it) }

        return async(Dispatchers.IO) task@ {
            get(url).use { response ->
                val data = response.body?.use { it.string() } ?: return@use emptyMap<String, Int>()
                return@task gson.fromJson<Map<String, Int>>(data, mapType)
            }
        }
    }

    /** 15周年ページのはてな全体のエントリリストを取得する */
    fun getHistoricalEntriesAsync(year: Int) : Deferred<List<Entry>> = async {
        val url = "$B_BASE_URL/15th/entries/$year.json"

        val hatenaHistoricalEntry = getJson<HatenaHistoricalEntry>(HatenaHistoricalEntry::class.java, url)
        val entries = hatenaHistoricalEntry.entries

        // ブクマ数は別途取得する必要がある
        val countsMap = getBookmarkCountsAsync(entries.map { it.canonicalUrl }).await()

        return@async entries.map {
            it.toEntry(count = countsMap[it.canonicalUrl])
        }
    }

    /** 15周年ページのランダムなユーザーエントリリストを取得する */
    fun getUserHistoricalEntriesAsync(year: Int, limit: Int = 10) : Deferred<List<Entry>> = async {
        require(signedIn()) { "need to sign-in for getting historical entries" }

        val url = buildString {
            append(
                B_BASE_URL, "/api/my/15th/yearly_random_bookmarks?",
                cacheAvoidance(),
                "&year=", year,
                "&limit=", limit
            )
        }

        val listType = object : TypeToken<List<UserHistoricalEntry>>(){}.type
        val entries = getJson<List<UserHistoricalEntry>>(listType, url)
        val userName = account!!.name

        return@async entries.map { it.toEntry(userName) }
    }

    /** ユーザーのツイートとそのクリック数を取得する(ユーザー固定、URL複数) */
    fun getTweetsAndClicksAsync(
        user: String,
        urls: List<String>
    ) : Deferred<List<TweetsAndClicks>> =
        getTweetsAndClicksImplAsync(
            urls.map {
                TweetsAndClicksRequestBody(url = it, user = user)
            }
        )

    /** ユーザーのツイートとそのクリック数を取得する(ユーザー複数、URL固定) */
    fun getTweetsAndClicksAsync(
        users: List<String>,
        url: String
    ) : Deferred<List<TweetsAndClicks>> =
        getTweetsAndClicksImplAsync(
            users.map {
                TweetsAndClicksRequestBody(url = url, user = it)
            }
        )

    /** ユーザーのツイートとそのクリック数を取得する(ユーザー複数、URL固定) */
    private fun getTweetsAndClicksImplAsync(
        params: List<TweetsAndClicksRequestBody>
    ) : Deferred<List<TweetsAndClicks>> = async {
        val apiUrl = "$B_BASE_URL/api/internal/bookmarks/tweets_and_clicks"
        val gson = GsonBuilder().create()
        val listType = object : TypeToken<List<TweetsAndClicks>>(){}.type

        val requestBody = gson.toJson(mapOf("bookmarks" to params)).toRequestBody()

        val request = Request.Builder()
            .post(requestBody)
            .url(apiUrl)
            .header("Content-Type", "application/json")
            .build()

        send(listType, request, GsonBuilder())
    }

    /** サイトタイトルを取得する */
    suspend fun getSiteTitle(url: String) : String = withContext(Dispatchers.IO) {
        try {
            val connection = Jsoup.connect(url)
            val root = connection.get()

            val titleTag = root.getElementsByTag("title").firstOrNull()
            titleTag?.wholeText() ?: ""
        }
        catch (e: HttpStatusException) {
            throw if (e.statusCode == 404) NotFoundException()
                  else ConnectionFailureException()
        }
        catch (e: Throwable) {
            throw ConnectionFailureException("connection failed: $url")
        }
    }

    /** はてなブログタグを取得する */
    suspend fun getKeyword(word: String) : List<Keyword> = withContext(Dispatchers.IO) {
        val url = "https://d.hatena.ne.jp/keyword/$word"
        try {
            val connection = Jsoup.connect(url)
            val root = connection.get()

            val tagBody = root.getElementById("tag-body")
            val items = tagBody.allElements.filter { item ->
                item.tagName() == "div" && item.children().any { it.tagName() == "header" }
            }

            return@withContext items.map { item ->
                val header = item.getElementsByTag("header").first()
                val title = header.getElementsByTag("h1").first().wholeText()
                val headerExtras = header.getElementsByTag("div")
                val category = headerExtras[1].wholeText()
                val kana = headerExtras[2].wholeText()

                val body = item.children().firstOrNull { it.tagName() == "div" }
                val bodyHtml = body?.html() ?: ""
                val bodyText = body?.wholeText() ?: ""
                Keyword(title, kana, category, bodyText, bodyHtml)
            }
        }
        catch (e: HttpStatusException) {
            throw if (e.statusCode == 404) NotFoundException()
            else ConnectionFailureException()
        }
        catch (e: Throwable) {
            throw ConnectionFailureException("connection failed: $url")
        }
    }
}
