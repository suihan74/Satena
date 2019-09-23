package com.suihan74.satena

import android.graphics.Color
import com.google.gson.GsonBuilder
import com.suihan74.satena.models.TaggedUser
import com.suihan74.satena.models.UserTag
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
    fun userTags() {
        val user0 = TaggedUser(0, "suihan")
        val user1 = TaggedUser(1, "hoge")
        val user2 = TaggedUser(2, "hage")

        val group = UserTag(
            0,
            "test",
            Color.BLACK
        ).apply {
            add(user0)
            add(user1)
            add(user2)
        }

        val gson = GsonBuilder().create()
        val json = gson.toJson(group)

        val dest = gson.fromJson(json, UserTag::class.java)
        assertEquals(group.name, dest.name)
        assertEquals(true, dest.contains(user0))
        assertEquals(true, dest.contains(user1))
        assertEquals(true, dest.contains(user2))
    }

    @Test
    fun userTagsContainer() {
        val container = UserTagsContainer()
        container.apply {
            addUser("suihan74")
            addUser("hoge")
            addUser("john")
        }

        container.apply {
            addTag("ネトウヨ")
            addTag("はてサ")
        }

        container.apply {
            tagUser(getUser("suihan74")!!, getTag("ネトウヨ")!!)
        }

        val gson = GsonBuilder().create()
        val json = gson.toJson(container)

        val dest = gson.fromJson(json, UserTagsContainer::class.java)
        assertEquals(true, dest.containsUser("suihan74"))
        assertEquals(true, dest.containsUser("hoge"))
        assertEquals(true, dest.containsUser("john"))
        assertEquals(true, dest.getTagsOfUser("suihan74").any { it.name == "ネトウヨ" })
    }
}
