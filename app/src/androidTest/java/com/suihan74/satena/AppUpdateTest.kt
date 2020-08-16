package com.suihan74.satena

import org.junit.Assert
import org.junit.Test

class AppUpdateTest {
    @Test
    fun minorVersionComparison() {
        val upperMask = 100000000
        val lowerMask = 1000000

        val current : Long = 113100000
        val currentMinor = (current % upperMask) / lowerMask

        Assert.assertEquals(13, currentMinor)
    }
}
