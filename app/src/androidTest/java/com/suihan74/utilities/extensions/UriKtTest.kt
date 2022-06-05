package com.suihan74.utilities.extensions

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test

class UriKtTest {

    @Test
    fun getEstimatedHierarchy1() {
        val actual = Uri.parse("https://b.hatena.ne.jp/suihan74/").estimatedHierarchy
        assertEquals("b.hatena.ne.jp/suihan74", actual)
    }

    @Test
    fun getEstimatedHierarchy2() {
        val actual = Uri.parse("https://b.hatena.ne.jp/").estimatedHierarchy
        assertEquals("b.hatena.ne.jp", actual)
    }

    @Test
    fun getEstimatedHierarchy2_2() {
        val actual = Uri.parse("https://b.hatena.ne.jp").estimatedHierarchy
        assertEquals("b.hatena.ne.jp", actual)
    }

    @Test
    fun getEstimatedHierarchy3() {
        val actual = Uri.parse("https://b.hatena.ne.jp/foo/bar/baz").estimatedHierarchy
        assertEquals("b.hatena.ne.jp/foo/bar", actual)
    }

    @Test
    fun getEstimatedHierarchy3_2() {
        val actual = Uri.parse("https://b.hatena.ne.jp/foo/bar/baz/").estimatedHierarchy
        assertEquals("b.hatena.ne.jp/foo/bar", actual)
    }
}
