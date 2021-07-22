package com.suihan74.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkCommentDecoratorTest {

    @Test
    fun convert_大文字() {
        val result = BookmarkCommentDecorator.convert("ID:test")
        assertEquals("test", result.ids.first())
    }

    @Test
    fun convert_大文字小文字1() {
        val result = BookmarkCommentDecorator.convert("Id:test")
        assertEquals("test", result.ids.first())
    }

    @Test
    fun convert_大文字小文字2() {
        val result = BookmarkCommentDecorator.convert("iD:test")
        assertEquals("test", result.ids.first())
    }

    @Test
    fun convert_空白含む1() {
        val result = BookmarkCommentDecorator.convert("id: test")
        assertEquals("test", result.ids.first())
    }

    @Test
    fun convert_空白含む2() {
        val result = BookmarkCommentDecorator.convert("id :test")
        assertEquals("test", result.ids.first())
    }

    @Test
    fun convert_空白含む3() {
        val result = BookmarkCommentDecorator.convert("id : test")
        assertEquals("test", result.ids.first())
    }
}
