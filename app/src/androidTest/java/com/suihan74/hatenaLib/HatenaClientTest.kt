package com.suihan74.hatenaLib

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class HatenaClientTest {
    @Test
    fun getRelatedEntries() = runBlocking {
        val url = "https://president.jp/articles/-/51543"
        val response = HatenaClient.getRelatedEntries(url)
        assertEquals(response.count(), 6)
    }

    @Test
    fun getRelatedEntries_of_unknown_entry() = runBlocking {
        val url = "https://b.hatena.ne.jp/entry/s/suihan74.github.io/"
        val response = HatenaClient.getRelatedEntries(url)
        assertEquals(response.count(), 0)
    }
}
