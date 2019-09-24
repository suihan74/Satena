package com.suihan74.satena

import com.suihan74.satena.models.UserTagsContainer
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

    @Test
    fun copyUserTag() {
        val container = UserTagsContainer()
        container.apply {
            addUser("suihan74")
            addUser("hoge")
            addUser("john")
        }

        container.apply {
            addTag("aaa")
            addTag("bbb")
        }

        val suihan = container.getUser("suihan74")!!
        val tag0 = container.getTag("aaa")!!

        container.apply {
            tagUser(suihan, tag0)
        }

        val tag0Copy = container.changeTagName(tag0, "ccc")
        assertEquals(true, tag0Copy.contains(suihan))
        assertEquals("ccc", tag0Copy.name)
    }

    @Test
    fun UserTagContainerCompaction() {
        val container = UserTagsContainer()
        val suihan = container.addUser("suihan")
        val hoge = container.addUser("hoge")
        val hage = container.addUser("hage")
        val john = container.addUser("john")
        val johnny = container.addUser("johnny")

        val tag0 = container.addTag("aaa")
        val tag1 = container.addTag("bbb")

        container.apply {
            tagUser(suihan, tag0)
            tagUser(hage, tag0)
            tagUser(john, tag1)
            tagUser(johnny, tag0)
            tagUser(johnny, tag1)
        }

        container.optimize()

        val newTag0 = container.getTag("aaa")!!
        val newJohnny = container.getUser("johnny")!!

        assertEquals(false, johnny == newJohnny)
        assertEquals(true, newJohnny.containsTag(newTag0))
    }
}




