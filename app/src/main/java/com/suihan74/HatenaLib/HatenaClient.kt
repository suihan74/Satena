package com.suihan74.HatenaLib

import android.net.Uri
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import java.io.IOException
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.net.*
import java.nio.charset.Charset
import java.util.*

/////////////////////////////////////////////////////////////////

object HatenaClient : BaseClient() {
    internal const val W_BASE_URL = "https://www.hatena.ne.jp"
    internal const val B_BASE_URL = "https://b.hatena.ne.jp"
    internal const val S_BASE_URL = "https://s.hatena.ne.jp"

    /** はてなブックマークの情報取得にログイン情報が必要なリクエストに付加するクッキー */
    private var mRk : HttpCookie? = null
    /** はてなスターの情報取得にログイン情報が必要なリクエストに付加するキー */
    private var mRksForStar : String? = null

    /** 現在ログイン済みのユーザ情報 */
    var account : Account? = null
        get() {
            synchronized(this) {
                return field
            }
        }
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
    private var ignoredUsersLastUpdated = LocalDateTime.MIN
    /** 非表示ユーザーリストのキャッシュを保持する時間 */
    private val ignoredUsersUpdateIntervals = Duration.ofMinutes(3)


    /** スター関連のjsonデシリアライザ */
    private fun getGsonBuilderForStars() =
         GsonBuilder()
            .registerTypeAdapter(Star::class.java, StarDeserializer())
            .registerTypeAdapter(StarColor::class.java, StarColorDeserializer())

    /**
     * HatenaClientがログイン済みか確認する
     */
    fun signedIn() : Boolean = mRk != null && account != null

    /**
     * HatenaClientがはてなスターのサービスにログイン済みかを確認する
     */
    fun signedInStar() : Boolean = mRksForStar != null

    /**
     * ログイン
     */
    fun signInAsync(name: String, password: String) : Deferred<Account> = GlobalScope.async {
        if (signedIn()) {
            signOut()
        }

        val url = "$W_BASE_URL/login"

        val response = post(url, mapOf(
            "name" to name,
            "password" to password
        ))
        if (response.isSuccessful) {
            val cookies = mCookieManager.cookieStore.cookies
            cookies.firstOrNull { it.name == "rk" }?.let {
                mRk = it

                response.close()
                try {
                    account = getAccountAsync().await()

                    listOf(
                        signInStarAsync(),
                        getIgnoredUsersAsync()
                    ).awaitAll()
                }
                catch (e: Exception) {
                    throw e
                }

                return@async account!!
            }
        }

        response.close()
        throw RuntimeException("connection error")
    }

    /**
     * クッキーを使って再ログイン
     */
    fun signInAsync(b: HttpCookie, rk: HttpCookie) : Deferred<Account> = GlobalScope.async {
        if (signedIn()) {
            signOut()
        }

        val url = "$W_BASE_URL/login"

        mCookieManager.cookieStore.add(URI(b.domain), b)
        mCookieManager.cookieStore.add(URI(rk.domain), rk)

        val response = get(url)
        if (response.isSuccessful) {
            val cookies = mCookieManager.cookieStore.cookies
            cookies
                .firstOrNull {
                    it.name == "rk"
                }
                .let {
                    mRk = it

                    response.close()
                    account = getAccountAsync().await()

                    listOf(
                        signInStarAsync(),
                        getIgnoredUsersAsync()
                    ).awaitAll()

                    return@async account!!
                }
        }

        response.close()
        throw RuntimeException("connection error")
    }

    /**
     * はてなスターのサービスを利用するためのキーを取得する
     */
    private fun signInStarAsync() : Deferred<Any> = GlobalScope.async {
        if (!signedIn()) throw RuntimeException("need to sign-in to use the star service")
        val url = "$S_BASE_URL/entries.json?${cacheAvoidance()}"
        val response = getJson<StarsEntries>(StarsEntries::class.java, url)

        mRksForStar = response.rks ?: throw RuntimeException("connection error: $S_BASE_URL")
    }


    /**
     * ログアウト
     */
    fun signOut() {
        mCookieManager.cookieStore.removeAll()
        mRk = null
        account = null
        ignoredUsers = emptyList()
    }

    /**
     * アカウント情報を取得
     */
    fun getAccountAsync() : Deferred<Account> = GlobalScope.async {
        // signedIn() = mRk != null && account != null
        if (mRk == null) throw RuntimeException("need to sign-in to get account")

        val url = "$B_BASE_URL/my.name"
        account = getJson<Account>(Account::class.java, url)
        return@async account!!
    }

    /**
     * 非表示ユーザーリストを取得（部分的に取得可能）
     */
    fun getIgnoredUsersAsync(limit: Int?, cursor: String?) : Deferred<IgnoredUsersResponse> = GlobalScope.async {
        if (!signedIn()) throw RuntimeException("need to sign-in to get ignored users")

        val urlBuilder = StringBuilder("$B_BASE_URL/api/my/ignore_users?${cacheAvoidance()}")
        if (limit == null) {
            val count = account!!.ignoresRegex.count { it == '|' } * 2
            urlBuilder.append("&limit=$count")
        }
        else {
            urlBuilder.append("&limit=$limit")
        }
        if (cursor != null) {
            urlBuilder.append("&cursor=$cursor")
        }

        val response = getJson<IgnoredUsersResponse>(IgnoredUsersResponse::class.java, urlBuilder.toString())
        ignoredUsers = response.users

        return@async response
    }

    /**
     * 非表示ユーザーリストを取得
     */
    fun getIgnoredUsersAsync(forciblyUpdate: Boolean = false) : Deferred<List<String>> = GlobalScope.async {
        if (forciblyUpdate
            || Duration.between(ignoredUsersLastUpdated, LocalDateTime.now()) > ignoredUsersUpdateIntervals
        ) {
            try {
                getIgnoredUsersAsync(null, null).await()
            }
            catch (e: RuntimeException) {
                throw e
            }
            catch (e: Exception) {
                throw RuntimeException(e.message)
            }
            finally {
                ignoredUsersLastUpdated = LocalDateTime.now()
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
    ) : Deferred<BookmarkResult> = GlobalScope.async {
        if (!signedIn()) throw RuntimeException("need to login for bookmarking")

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

        val response = post(apiUrl, params)
        if (response.isSuccessful) {
            val gson = GsonBuilder()
                .serializeNulls()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Boolean::class.java, BooleanDeserializer())
                .create()
            val json = response.body()!!.string()
            val result = gson.fromJson<BookmarkResult>(json, BookmarkResult::class.java)

            response.close()
            return@async result
        }

        response.close()
        throw RuntimeException("failed to bookmark")
    }

    /**
     * 対象urlのブックマークを削除する
     */
    fun deleteBookmarkAsync(url: String) : Deferred<Any> = GlobalScope.async {
        if (!signedIn()) throw RuntimeException("need to login for deleting bookmarks")
        val account = account!!
        val apiUrl = "$B_BASE_URL/${account.name}/api.delete_bookmark.json"

        try {
            post(apiUrl, mapOf(
                "url" to url,
                "rks" to account.rks))
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
    ) : Deferred<List<Entry>> = GlobalScope.async {
        if (!signedIn()) throw RuntimeException("need to sign-in to get user's bookmarked entries")

        val urlBuilder = StringBuilder("$B_BASE_URL/api/ipad.mybookmarks?${cacheAvoidance()}")

        if (limit != null) {
            urlBuilder.append("&limit=$limit")
        }
        if (of != null) {
            urlBuilder.append("&of=$of")
        }

        val listType = object : TypeToken<List<Entry>>() {}.type
        return@async getJson<List<Entry>>(listType, urlBuilder.toString())
    }

    /**
     * 自分がブクマしたエントリを検索
     */
    fun searchMyEntriesAsync(
        query: String,
        searchType: SearchType,
        limit: Int? = null,
        of: Int? = null) : Deferred<List<Entry>> = GlobalScope.async {
        if (!signedIn()) throw RuntimeException("need to sign-in to search user's bookmarked entries")

        val urlBuilder = StringBuilder("$B_BASE_URL/api/ipad.mysearch/${searchType.name.toLowerCase()}?${cacheAvoidance()}&q=$query")
        if (limit != null) {
            urlBuilder.append("&limit=$limit")
        }
        if (of != null) {
            urlBuilder.append("&of=$of")
        }

        val url = urlBuilder.toString()
        val listType = object : TypeToken<List<Entry>>() {}.type
        return@async getJson<List<Entry>>(listType, url)
    }

    /**
     * マイホットエントリを取得
     */
    fun getMyHotEntriesAsync(date: String? = null) : Deferred<List<Entry>> = GlobalScope.async {
        val urlBuilder = StringBuilder("$B_BASE_URL/api/entries/myhotentry.json?include_amp_urls=1")
        if (!date.isNullOrEmpty()) {
            urlBuilder.append("&date=$date")
        }
        val url = urlBuilder.toString()
        val listType = object : TypeToken<List<Entry>>() {}.type
        return@async getJson<List<Entry>>(listType, url)
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

        if (category == Category.MyBookmarks) {
            return getMyBookmarkedEntriesAsync(limit ?: 50, of ?: 0)
        }
        if (category == Category.MyHotEntries) {
            return getMyHotEntriesAsync()
        }

        return GlobalScope.async {
            val target = when (entriesType) {
                EntriesType.Recent -> "newentry"
                EntriesType.Hot -> "hotentry"
            }
            val urlBuilder = StringBuilder("$B_BASE_URL/api/ipad.$target.json?${cacheAvoidance()}" +
                    "&category_id=$category" +
                    "&include_amp_urls=${includeAMPUrls.int}" +
                    "&include_bookmarked_data=${includeBookmarkedData.int}" +
                    "&include_bookmarks_of_followings=${includeBookmarksOfFollowings.int}" +
                    "&ad=${ad.int}")
            if (limit != null) urlBuilder.append("&limit=$limit")
            if (of != null) urlBuilder.append("&of=$of")

            val url = urlBuilder.toString()
            val listType = object : TypeToken<List<Entry>>() {}.type
            return@async getJson<List<Entry>>(listType, url)
        }
    }

    /**
     * 指定ユーザーがブクマしたエントリを取得する
     */
    fun getUserEntriesAsync(
        user: String,
        limit: Int? = null,
        of: Int? = null
    ) : Deferred<List<Entry>> = GlobalScope.async {
        val urlBuilder = StringBuilder("$B_BASE_URL/api/internal/user/$user/bookmarks?${cacheAvoidance()}")
        if (limit != null) {
            urlBuilder.append("&limit=$limit")
        }
        if (of != null) {
            urlBuilder.append("&offset=$of")
        }

        val url = urlBuilder.toString()
        val response = getJson<UserEntryResponse>(UserEntryResponse::class.java, url)
        return@async response.bookmarks.map { it.toEntry() }
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
    ) : Deferred<List<Entry>> = GlobalScope.async {
        val urlBuilder = StringBuilder("$B_BASE_URL/api/ipad.search/${searchType.name.toLowerCase()}?${cacheAvoidance()}").apply {
            append("&q=${Uri.encode(query)}")
            append("&sort=${entriesType.name.toLowerCase()}")
            append("&include_bookmarked_data=1")
        }
        if (limit != null) {
            urlBuilder.append("&limit=$limit")
        }
        if (of != null) {
            urlBuilder.append("&of=$of")
        }

        val url = urlBuilder.toString()

        val listType = object : TypeToken<List<Entry>>() {}.type
        return@async getJson<List<Entry>>(listType, url)
    }

    /**
     * ブックマーク情報を取得する
     */
    fun getBookmarksEntryAsync(url: String) : Deferred<BookmarksEntry> = GlobalScope.async {
        val apiUrl = "$B_BASE_URL/entry/jsonlite/?url=${Uri.encode(url)}&${cacheAvoidance()}"
        return@async getJson<BookmarksEntry>(BookmarksEntry::class.java, apiUrl, "yyyy/MM/dd HH:mm")
    }

    /**
     * まだ誰にもブックマークされていないページのダミーブックマーク情報を作成する
     */
    fun getEmptyBookmarksEntryAsync(url: String) : Deferred<BookmarksEntry> = GlobalScope.async {
        val response = get(url)
        val title = if (response.isSuccessful) {
            val bodyBytes = response.body()!!.bytes()

            // 文字コードを判別してからHTMLを読む
            val defaultCharsetName = Charset.defaultCharset().name().toLowerCase()
            var charsetName = defaultCharsetName
            var charsetDetected = false

            val charsetRegex = Regex("""charset=([a-zA-Z0-9_\-]+)""")
            fun parseCharset(src: String) : String {
                val matchResult = charsetRegex.find(src)
                return if (matchResult?.groups?.size ?: 0 >= 2) matchResult!!.groups[1]!!.value.toLowerCase() else ""
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
                rawDoc.getElementsByTag("meta").let {
                    // <meta charset="???">
                    val charsetMeta = it.find { it.hasAttr("charset") }
                    if (charsetMeta != null) {
                        charsetName = charsetMeta.attr("charset").toLowerCase()
                        charsetDetected = true
                        return@let
                    }

                    // <meta http-equiv="Content-Type" content="text/html; charset=???">
                    val meta = it.find { it.attr("http-equiv")?.toLowerCase() == "content-type" }
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

            val titleElement = if (charsetName == defaultCharsetName) {
                rawDoc.select("title")
            }
            else {
                val doc = Jsoup.parse(bodyBytes.inputStream(), charsetName, url)
                doc.select("title")
            }

            titleElement?.html() ?: ""
        }
        else { "" }
        response.close()

        return@async BookmarksEntry(
            id = 0,
            title = title,
            bookmarks = emptyList(),
            count = 0,
            url = url,
            entryUrl = url,
            screenshot = "")
    }

    /**
     * ブックマークリストを取得する
     */
    fun getRecentBookmarksAsync(
        url: String,
        limit: Long? = null,
        of: Long? = null
    ) : Deferred<List<BookmarkWithStarCount>> = GlobalScope.async {
        var apiUrl = "$B_BASE_URL/api/ipad.entry_bookmarks?${cacheAvoidance()}&url=${Uri.encode(url)}"
        if (limit != null) {
            apiUrl += "&limit=$limit"
        }
        if (of != null) {
            apiUrl += "&of=$of"
        }
        val listType = object : TypeToken<List<BookmarkWithStarCount>>() {}.type
        return@async getJson<List<BookmarkWithStarCount>>(listType, apiUrl)
    }

    /**
     * ブックマーク概要を取得する
     */
    fun getDigestBookmarksAsync(url: String, limit: Long? = null) : Deferred<BookmarksDigest> = GlobalScope.async {
        var apiUrl = "$B_BASE_URL/api/ipad.entry_reactions?url=${Uri.encode(url)}"
        if (limit != null) {
            apiUrl += "&limit=$limit"
        }
        return@async getJson<BookmarksDigest>(BookmarksDigest::class.java, apiUrl)
    }

    /**
     * エントリーIDから元の記事のURLを取得する
     */
    fun getEntryUrlFromIdAsync(eid: Long) : Deferred<String> = GlobalScope.async {
        val url = "$B_BASE_URL/entry/$eid"
        val request = Request.Builder().url(url).build()
        val call = mClient.newCall(request)

        val response = call.execute()
        if (response.isSuccessful) {
            val commentPageUrl = response.request().url().toString()
            response.close()

            val headHttps = "$B_BASE_URL/entry/s/"
            val head = "$B_BASE_URL/entry/"

            val isHttps = commentPageUrl.startsWith(headHttps)
            val tail = commentPageUrl.substring(if (isHttps) headHttps.length else head.length)
            val entryUrl = "${if (isHttps) "https" else "http"}://$tail"
            return@async entryUrl
        }

        response.close()
        throw RuntimeException("connection error")
    }

    /**
     * エントリーIDからブックマーク情報を取得する
     */
    fun getBookmarksEntryAsync(eid: Long) : Deferred<BookmarksEntry> = GlobalScope.async {
        val url = getEntryUrlFromIdAsync(eid).await()
        return@async getBookmarksEntryAsync(url).await()
    }

    /**
     * ブックマーク概要を取得する
     */
    fun getDigestBookmarksAsync(eid: Long, limit: Long? = null) : Deferred<BookmarksDigest> = GlobalScope.async {
        val url = getEntryUrlFromIdAsync(eid).await()
        return@async HatenaClient.getDigestBookmarksAsync(url, limit).await()
    }

    /**
     * スター情報を複数一括で取得する
     */
    fun getStarsEntryAsync(urls: Iterable<String>) : Deferred<List<StarsEntry>> = GlobalScope.async {
        val apiBaseUrl = "$S_BASE_URL/entry.json?${cacheAvoidance()}&uri="
        val params = urls.map { Uri.encode(it) }.joinToString("&uri=")
        var apiUrl = apiBaseUrl + params

        val gsonBuilder = getGsonBuilderForStars()
        var isSuccess = true

        val limit = 2000

        if (apiUrl.length > limit) {
            val tasks = ArrayList<Deferred<StarsEntries?>>()
            while (apiUrl.length > limit) {
                val left = apiUrl.substring(0 until limit)
                var lastSeparatorIndex = left.lastIndexOf('&')
                if (lastSeparatorIndex < 0) lastSeparatorIndex = left.length

                val curApiUrl = left.substring(0 until lastSeparatorIndex)
                apiUrl =
                    apiBaseUrl + (if (lastSeparatorIndex < left.length - 5) left.substring(lastSeparatorIndex + 5) else "") + apiUrl.substring(
                        limit
                    )
                // 5 means "&uri=".length

                tasks.add(async {
                    try {
                        return@async getJson<StarsEntries>(StarsEntries::class.java, curApiUrl, gsonBuilder)
                    }
                    catch (e: SocketTimeoutException) {
                        return@async null
                    }
                })
            }

            tasks.add(async {
                try {
                    return@async getJson<StarsEntries>(StarsEntries::class.java, apiUrl, gsonBuilder)
                }
                catch (e: SocketTimeoutException) {
                    isSuccess = false
                    return@async null
                }
            })

            if (!isSuccess) throw SocketTimeoutException("timeout")

            return@async tasks
                .map { it.await() }
                .filter { it != null }
                .flatMap { it!!.entries }
        }
        else {
            val response = getJson<StarsEntries>(StarsEntries::class.java, apiUrl, gsonBuilder)
            return@async response.entries
        }
    }

    /**
     * スター情報を取得する
     */
    fun getStarsEntryAsync(url: String) : Deferred<StarsEntry> = GlobalScope.async {
        val apiUrl = "$S_BASE_URL/entry.json?uri=${Uri.encode(url)}&${cacheAvoidance()}"
        val gsonBuilder = getGsonBuilderForStars()
        val response = getJson<StarsEntries>(StarsEntries::class.java, apiUrl, gsonBuilder)
        return@async response.entries[0]
    }

    /**
     * 対象URLにスターをつける
     */
    fun postStarAsync(url: String, color: StarColor = StarColor.Yellow, quote: String = "") : Deferred<Star> = GlobalScope.async {
        if (!signedInStar()) throw RuntimeException("need to sign-in to post star")
        val apiUrl = "$S_BASE_URL/star.add.json?${cacheAvoidance()}" +
                "&uri=${Uri.encode(url)}" +
                "&rks=$mRksForStar" +
                "&color=${color.name.toLowerCase()}" +
                "&quote=${Uri.encode(quote)}"
        return@async getJson<Star>(Star::class.java, apiUrl)

/*

        if (!signedIn()) throw RuntimeException("need to sign-in to get color stars")

        val apiUrl = "$S_BASE_URL/api/v0/entry/stars"

/*
        val body = FormBody.Builder()
            .add("uri", url)
            .add("quote", quote ?: "")
            .add("color", if (color == StarColor.Yellow) "" else color.name.toLowerCase())
            .build()
*/
        val obj = JSONObject().apply {
            put("uri", url)
            put("quote", quote)
        }
        val json = obj.toString()

        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json)

        val request = Request.Builder()
            .url(apiUrl)
            .header("Content-Type", "application/json")
            .header("X-Internal-API-Key", "wvlYJXSGDMY161Bbw4TEf8unWl4pDLLB1gy7PGcA")
            .header("X-Internal-API-RK", mRk!!.value)
            .header("User-Agent", "Bookmark/4.27 (Android)")
            .post(body)
            .build()

        val response = send(request)
        if (response.isSuccessful) {
            return@async Star(
                user = account!!.name,
                quote = quote,
                color = color)
        }

        throw RuntimeException("connection error : ${response.code()} ${response.message()}")
        */
    }

    fun deleteStarAsync(url: String, star: Star) : Deferred<Any> = GlobalScope.async {
        if (!signedInStar()) throw RuntimeException("need to sign-in to delete star")
        val apiUrl = "$S_BASE_URL/star.delete.json?${cacheAvoidance()}" +
                "&uri=${Uri.encode(url)}" +
                "&rks=$mRksForStar" +
                "&name=${star.user}" +
                "&color=${star.color.name.toLowerCase()}" +
                "&quote=${Uri.encode(star.quote)}"
        val response = get(apiUrl)
        if (!response.isSuccessful) throw RuntimeException("failed to delete a star")
    }

    /**
     *  ログインユーザーが持っているカラースターの情報を取得する
     */
    fun getMyColorStarsAsync() : Deferred<UserColorStarsCount> = GlobalScope.async {
        if (!signedIn()) throw RuntimeException("need to sign-in to get color stars")

        val url = "$S_BASE_URL/api/v0/me/colorstars"

        val request = Request.Builder()
            .get()
            .url(url)
            .header("Content-Type", "application/json")
            .header("X-Internal-API-Key", "wvlYJXSGDMY161Bbw4TEf8unWl4pDLLB1gy7PGcA")
            .header("X-Internal-API-RK", mRk!!.value)
            .build()

        val response = send<UserColorStarsResponse>(UserColorStarsResponse::class.java, request)
        if (response.success) {
            return@async response.result["counts"] ?: UserColorStarsCount(0, 0, 0, 0)
        }

        throw RuntimeException("connection error")
    }

    /**
     * 通知を取得する
     */
    fun getNoticesAsync() : Deferred<NoticeResponse> = GlobalScope.async {
        if (!signedIn()) throw RuntimeException("need to sign-in to get user's notices")
        val url = "$W_BASE_URL/notify/api/pull?${cacheAvoidance()}"
        return@async getJson<NoticeResponse>(NoticeResponse::class.java, url)
    }

    /**
     * 通知既読状態を更新する
     */
    fun updateNoticesLastSeenAsync() : Deferred<Any> = GlobalScope.async {
        if (!signedIn()) throw RuntimeException("need to sign-in to update notices last seen")
        val url = "$W_BASE_URL/notify/api/read"

        try {
            val response = post(url, mapOf("rks" to account!!.rks))
            if (!response.isSuccessful) {
                throw RuntimeException("failed to update notices last seen")
            }
        }
        catch (e: IOException) {
            throw RuntimeException("failed to update notices last seen")
        }
    }

    /**
     * 指定ユーザーが使用しているタグを取得する
     */
    fun getUserTagsAsync(user: String) : Deferred<List<Tag>> = GlobalScope.async {
        val url = "$B_BASE_URL/$user/tags.json?${cacheAvoidance()}"
        val response = getJson<TagsResponse>(TagsResponse::class.java, url)
        if (response.status != 200) throw RuntimeException("failed to get $user's tags: ${response.status}")
        return@async response.tags
            .map { Tag(it.key, it.value.index, it.value.count, it.value.timestamp) }
            .sortedBy { it.index }
            .toList()
    }

    /**
     * 指定したユーザーのブコメを非表示にする
     */
    fun ignoreUserAsync(user: String) : Deferred<Any> = GlobalScope.async {
        if (!signedIn()) throw RuntimeException("need to sign-in to mute users")
        val url = "$B_BASE_URL/${account!!.name}/api.ignore.json"
        try {
            val response = post(url, mapOf(
                "username" to user,
                "rks" to account!!.rks
            ))
            if (!response.isSuccessful) throw RuntimeException("failed to mute an user: $user")
            if (!ignoredUsers.contains(user)) {
                ignoredUsers = ignoredUsers.plus(user)
            }
        }
        catch (e: IOException) {
            throw RuntimeException("failed to mute an user: $user")
        }
    }

    /**
     * 指定したユーザーのブコメ非表示を解除する
     */
    fun unignoreUserAsync(user: String) : Deferred<Any> = GlobalScope.async {
        if (!signedIn()) throw RuntimeException("need to sign-in to mute users")
        val url = "$B_BASE_URL/${account!!.name}/api.unignore.json"
        try {
            val response = post(url, mapOf(
                "username" to user,
                "rks" to account!!.rks
            ))
            if (!response.isSuccessful) throw RuntimeException("failed to mute an user: $user")
            ignoredUsers = ignoredUsers.filterNot { it == user }.toList()
        }
        catch (e: IOException) {
            throw RuntimeException("failed to mute an user: $user")
        }
    }

    /**
     * ログインユーザーが使用しているタグを取得する
     */
    fun getUserTagsAsync() : Deferred<List<Tag>> {
        if (!signedIn()) throw RuntimeException("need to sign-in to get user's tags")
        return getUserTagsAsync(account!!.name)
    }

    /**
     * ユーザー名からアイコンURLを取得する
     */
    fun getUserIconUrl(user: String) : String = "https://cdn1.www.st-hatena.com/users/$user/profile.gif"

    /**
     * URLがブコメページのURLかを判別する
     */
    fun isUrlCommentPages(url: String) : Boolean = url.startsWith("$B_BASE_URL/entry/")

    /**
     * ブコメページのURLからエントリのURLを取得する
     * e.g.)  https://b.hatena.ne.jp/entry/s/www.hoge.com/ ==> https://www.hoge.com/
     */
    fun getEntryUrlFromCommentPageUrl(url: String) : String {
        val regex = Regex("""https?:\/\/b\.hatena\.ne\.jp\/entry\/(https:\/\/|s\/)?(.+)""")
        val matches = regex.matchEntire(url) ?: throw RuntimeException("invalid comment page url: $url")

        val path = matches.groups[2]?.value ?: throw RuntimeException("invalid comment page url: $url")

        return if (matches.groups[1]?.value.isNullOrEmpty()) {
            if (path.startsWith("http://")) {
                path
            }
            else {
                "http://$path"
            }
        }
        else {
            "https://$path"
        }
    }

    /**
     * エントリのURLからブコメページのURLを取得する
     * e.g.)  https://www.hoge.com/ ===> https://b.hatena.ne.jp/entry/s/www.hoge.com/
     */
    fun getCommentPageUrlFromEntryUrl(url: String) : String {
        return "$B_BASE_URL/entry/" + if (url.startsWith("https://")) {
            "s/${url.substring("https://".length)}"
        }
        else if (url.startsWith("http://")) {
            url.substring("http://".length)
        }
        else {
            throw RuntimeException("invalid url: $url")
        }
    }
}
