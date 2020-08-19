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
        }
    }
}
