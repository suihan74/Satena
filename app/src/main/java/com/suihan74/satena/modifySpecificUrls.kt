package com.suihan74.satena

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * ブックマーク情報が正常に取得できるURLに修正する
 */
suspend fun modifySpecificUrls(url: String?) : String? = when {
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

    else -> withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .get()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                Jsoup.parse(response.body!!.string()).head()
                    .allElements
                    .firstOrNull { elem ->
                        elem.tagName() == "meta" && (elem.attr("property") == "og:url" || elem.attr("name") == "twitter:url")
                    }
                    ?.attr("content")
                    ?: url
            }
        }
        catch (e: Exception) {
            url
        }
    }
}
