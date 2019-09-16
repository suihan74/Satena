package com.suihan74.satena

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun removeUrlScheme() {
        val domain = "www.google.com/"
        val url = "https://$domain"
        val schemeRegex = Regex("""^https?://""")
        val matchResult = schemeRegex.find(url) ?: throw RuntimeException("failed...")
        val result = url.substring(matchResult.range.last + 1)
        assertEquals(domain, result)
    }
}
