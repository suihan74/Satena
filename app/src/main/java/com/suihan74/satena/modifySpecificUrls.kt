package com.suihan74.satena

import android.net.Uri
import com.suihan74.hatenaLib.HatenaClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * ブックマーク情報が正常に取得できるURLに修正する
 */
suspend fun modifySpecificUrls(url: String?) : String? =
    when (val modifiedTemp = modifySpecificUrlsWithoutConnection(url)) {
        null -> null
        url -> modifySpecificUrlsWithConnection(url)
        else -> modifiedTemp
    }


/**
 * 幾つかの頻出するサイトに対して
 * ブックマーク情報が正常に取得できるURLに修正する
 * (OGP検証など通信を必要とする補正は行わない)
 */
fun modifySpecificUrlsWithoutConnection(url: String?) : String? = when {
    url == null -> null

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
suspend fun modifySpecificUrlsWithConnection(url: String?) : String? = when (url) {
    null -> null
    else -> withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .get()
                .url(url)
                .build()

            val modified = client.newCall(request).execute().use { response ->
                val root = Jsoup.parse(response.body!!.string())
                val entryRegex = Regex("""https?://b\.hatena\.ne\.jp/entry/\d+/?$""")
                if (url.startsWith(HatenaClient.B_BASE_URL+"/entry")) {
                    if (entryRegex.matches(url)) {
                        // "https://b.hatena.ne.jp/entry/{eid}"は通常の法則に則ったURLのブコメページにリダイレクトされる
                        // modifySpecificUrls()では、さらにそのブコメページのブコメ先エントリURLに変換する
                        // 例) [in] /entry/18625960 ==> /entry/s/www.google.com ==> https://www.google.com [out]
                        root.getElementsByTag("html").first()
                            .attr("data-entry-url")
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
}
