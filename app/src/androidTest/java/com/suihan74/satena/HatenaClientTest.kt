package com.suihan74.satena

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.suihan74.hatenaLib.HatenaClient
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotSame
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class HatenaClientTest {
    @Test
    fun getEntryUrl_A() {
        val url0 = HatenaClient.getEntryUrlFromCommentPageUrl("https://b.hatena.ne.jp/entry/12345678")
        val url1 = HatenaClient.getEntryUrlFromCommentPageUrl("https://b.hatena.ne.jp/entry?eid=12345678")
        val url2 = HatenaClient.getEntryUrlFromCommentPageUrl("https://b.hatena.ne.jp/entry/12345678/comment/suihan74")

        assertEquals("url0 != url1", url0, url1)
        assertEquals("url0 != url2", url0, url2)
    }

    @Test
    fun getEntryUrl_B() {
        val url0 = HatenaClient.getEntryUrlFromCommentPageUrl("https://b.hatena.ne.jp/entry?url=https://suihan74.github.io/")
        val url1 = HatenaClient.getEntryUrlFromCommentPageUrl("https://b.hatena.ne.jp/entry/https://suihan74.github.io/")
        val url2 = HatenaClient.getEntryUrlFromCommentPageUrl("https://b.hatena.ne.jp/entry/s/suihan74.github.io/")

        assertEquals("url0 != url1", url0, url1)
        assertEquals("url0 != url2", url0, url2)
    }

    @Test
    fun fifteenth() {
        runBlocking {
            val entries = HatenaClient.getHistoricalEntriesAsync(2018).await()
            assertNotSame(0, entries.size)
            assert(entries.none { it.count == 0 })
        }
    }

    @Test
    fun tweetsAndClicks() {
        runBlocking {
            val urls = listOf(
                "https://nazology.net/archives/66899",
                "https://b.hatena.ne.jp/15th",
                "https://blog.jetbrains.com/ja/kotlin/2020/08/kotlin-1-4-released-with-a-focus-on-quality-and-performance-ja/",
                "https://developer.hatenastaff.com/entry/2020/08/14/093000",
                "https://qiita.com/mrok273/items/58532c06a3af3324d970",
                "https://hayashih.hatenablog.com/entry/2020/08/13/223424",
                "https://www.itmedia.co.jp/news/articles/2008/13/news059.html",
                "https://www.itmedia.co.jp/news/articles/2008/12/news048.html",
                "https://togetter.com/li/1573009",
                "https://anond.hatelabo.jp/20200809024637",
                "https://www.google.com/",
                "https://suihan74.github.io/posts/2020/08_06_01_kotlinx_serialization/",
                "https://magcomi.com/episode/13933686331659442449",
                "https://anond.hatelabo.jp/20200805001542",
                "https://suihan74.github.io/posts/2020/08_04_00_escape_minifying/",
                "https://www.itmedia.co.jp/news/articles/2008/03/news101.html",
                "https://store.steampowered.com/app/1029210/30XX/",
                "https://anond.hatelabo.jp/20200801034304",
                "https://suihan74.github.io/posts/2020/08_01_00_satena/",
                "https://news.yahoo.co.jp/articles/62637f76cd92e67dd183a10eb1059d6c244b4f1c"
            )
            val resultA = HatenaClient.getTweetsAndClicksAsync("suihan74", urls).await()

            val users = listOf(
                "suihan74",
                "sakamata",
                "nunnnunn",
                "oriak",
                "natsume_sanshir",
                "nyah"
            )
            val resultB = HatenaClient.getTweetsAndClicksAsync(
                users,
                "https://nazology.net/archives/66899"
            ).await()

            val a = false
        }
    }
}

