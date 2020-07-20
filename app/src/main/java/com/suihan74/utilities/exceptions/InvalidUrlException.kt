package com.suihan74.utilities.exceptions

/**
 * 与えられた文字列がURLとして不適当な場合に送出
 */
class InvalidUrlException(url: String? = null) : Throwable("invalid url: " + (url ?: "null"))
