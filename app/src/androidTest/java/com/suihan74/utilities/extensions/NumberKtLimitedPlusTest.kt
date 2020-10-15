@file:Suppress("NonAsciiCharacters")

package com.suihan74.utilities.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberKtLimitedPlusTest {
    @Test
    fun オーバーフローを防ぐ() {
        val a = Long.MAX_VALUE - 1
        val b = Long.MAX_VALUE
        assertEquals(Long.MAX_VALUE, a.limitedPlus(b))
    }

    @Test
    fun アンダーフローを防ぐ() {
        val a = Long.MIN_VALUE + 1
        val b = Long.MIN_VALUE
        assertEquals(Long.MIN_VALUE, a.limitedPlus(b))
    }

    @Test
    fun 任意の上限値() {
        val a = 10L
        val b = 20L
        val limit = 25L
        assertEquals(limit, a.limitedPlus(b, limit))
    }

    @Test
    fun 上限値に達しない() {
        val a = 10L
        val b = 2L
        val limit = 25L
        val expected = 12L
        assertEquals(expected, a.limitedPlus(b, limit))
    }

    @Test
    fun 値が変化しない() {
        val a = 10L
        val b = 0L
        val limit = 25L
        val expected = 10L
        assertEquals(expected, a.limitedPlus(b, limit))
    }
}
