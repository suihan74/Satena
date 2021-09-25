package com.suihan74.satena

import android.net.Uri
import com.suihan74.hatenaLib.EntriesType
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.SearchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * ブックマーク情報が正常に取得できるURLに修正する
 */
suspend fun modifySpecificUrls(url: String?) : String? {
    if (url == null) return null

    val result = runCatching {
        when (val modifiedTemp = modifySpecificUrlsWithoutConnection(url)) {
            "about:blank" -> modifiedTemp
            url -> runCatching { modifySpecificUrlsForEntry(url) }.getOrNull()
            else -> modifiedTemp
        }
    }

    return result.getOrDefault(url)
}

/**
 * 幾つかの頻出するサイトに対して
 * ブックマーク情報が正常に取得できるURLに修正する
 * (OGP検証など通信を必要とする補正は行わない)
 */
private fun modifySpecificUrlsWithoutConnection(url: String) : String = when {
    url == "about:blank" -> url

    url.startsWith("https://m.youtube.com/") ->
        Regex("""https://m\.youtube\.com/(.*)""").replace(url) { m ->
            "https://www.youtube.com/${m.groupValues.last()}"
        }

    url.startsWith("https://mobile.twitter.com/") ->
        Regex("""https://mobile\.twitter\.com/(.*)""").replace(url) { m ->
            "https://twitter.com/${m.groupValues.last()}"
        }

    url.startsWith("https://mobile.facebook.com/") ->
        Regex("""https://mobile\.facebook\.com/(.*)""").replace(url) { m ->
            "https://www.facebook.com/${m.groupValues.last()}"
        }

    else -> url
}

/**
 * ブックマーク情報が正常に取得できるURLに修正する
 * (eidから元URLを取得する・OGPタグやTwitterカードなどのmetaタグを参照する)
 */
private suspend fun modifySpecificUrlsWithConnection(url: String) : String = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .get()
            .url(url)
            .build()

        val modified = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use url

            val root = Jsoup.parse(response.body!!.use { it.string() })
            val entryRegex = Regex("""https?://b\.hatena\.ne\.jp/entry/\d+/?$""")
            if (url.startsWith(HatenaClient.B_BASE_URL+"/entry")) {
                if (entryRegex.matches(url)) {
                    // "https://b.hatena.ne.jp/entry/{eid}"は通常の法則に則ったURLのブコメページにリダイレクトされる
                    // modifySpecificUrls()では、さらにそのブコメページのブコメ先エントリURLに変換する
                    // 例) [in] /entry/18625960 ==> /entry/s/www.google.com ==> https://www.google.com [out]
                    val htmlTag = root.getElementsByTag("html").first()
                    if (htmlTag.attr("data-page-subtype") == "comment") {
                        // コメントページのURLの場合
                        htmlTag.attr("data-stable-request-url")
                    }
                    else {
                        htmlTag.attr("data-entry-url")
                    }
                }
                else {
                    url
                }
            }
            else {
                root.head()
                    .allElements
                    .firstOrNull { elem ->
                        elem.tagName() == "meta" && (elem.attr("property") == "og:url" || elem.attr(
                            "name"
                        ) == "twitter:url")
                    }
                    ?.attr("content")
            }
        } ?: url

        val modifiedUri = Uri.parse(modified)
        val urlUri = Uri.parse(url)

        if (modifiedUri.scheme != urlUri.scheme && url.removePrefix(urlUri.scheme ?: "") == modified.removePrefix(modifiedUri.scheme ?: "")) url
        else modified
    }
    catch (e: Throwable) {
        url
    }
}

/**
 * URLが既にエントリ登録済みかどうかを確認し、そうであるならそのURLを、
 * 未登録なら妥当なURLを推定して補正する
 */
private suspend fun modifySpecificUrlsForEntry(srcUrl: String) : String = withContext(Dispatchers.IO) {
    val modifiedUrl = modifySpecificUrlsWithoutConnection(srcUrl)
    val searchResult = HatenaClient.searchEntriesAsync(modifiedUrl, SearchType.Text, EntriesType.Recent).await()

    if (searchResult.any { it.url == modifiedUrl }) modifiedUrl
    else modifySpecificUrlsWithConnection(modifiedUrl)
}

// ------ //

/**
 * 与えられたURLに対応するエントリのrootUrlと思われるアドレスを取得する
 *
 * (大体の場合https://domain/)
 */
suspend fun getEntryRootUrl(srcUrl: String) : String {
    val modifiedUrl = modifySpecificUrls(srcUrl) ?: srcUrl

    val twitterRegex = Regex("""^(https://twitter\.com/[a-zA-Z0-9_]+/?)""")
    val twMatch = twitterRegex.find(modifiedUrl)

    val rootUrl =
        when {
            twMatch != null -> twMatch.groupValues[0]

            else -> runCatching {
                val uri = Uri.parse(modifiedUrl)
                uri.scheme + "://" + uri.authority + "/"
            }.getOrDefault(modifiedUrl)
        }

    return rootUrl
}
