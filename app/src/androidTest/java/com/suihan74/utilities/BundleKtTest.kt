package com.suihan74.utilities

import android.os.Bundle
import com.suihan74.utilities.extensions.getEnum
import com.suihan74.utilities.extensions.putEnum
import org.junit.Assert.assertEquals
import org.junit.Test

enum class TestEnum {
    A, B, C
}

enum class TestIntEnum(
    val int: Int
) {
    A(1),
    B(2),
    C(4);

    companion object {
        fun fromInt(i: Int) = values().firstOrNull { it.int == i } ?: A
    }
}

class BundleKtTest {
    @Test
    fun getEnumに値がnullのときの処理を追加() {
        val bundle = Bundle()
        val key = "test"
        val value = TestEnum.B

        bundle.putEnum(key, value)
        assertEquals(TestEnum.B, bundle.getEnum<TestEnum>(key))

        bundle.putEnum<TestEnum>(key, null)
        assertEquals(null, bundle.getEnum<TestEnum>(key))
    }

    @Test
    fun getEnumに値がnullのときの処理を追加_selector使用() {
        val bundle = Bundle()
        val key = "test2"
        val value = TestIntEnum.B

        bundle.putEnum(key, value) { it.int }
        assertEquals(TestIntEnum.B, bundle.getEnum<TestIntEnum>(key) { it.int })

        bundle.putEnum<TestIntEnum>(key, null) { it.int }
        assertEquals(null, bundle.getEnum<TestIntEnum>(key) { it.int })
    }
}
