package com.suihan74.utilities

import android.text.Spannable
import android.text.SpannableStringBuilder

/**
 * 装飾を施したテキストを追加する
 *
 * append(text, span)は以下のコードと等価
 *
 * val start = builder.length
 *
 * builder.append(text)
 *
 * builder.setSpan(span, start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
 */
fun SpannableStringBuilder.append(
    text: String,
    span: Any,
    start: Int? = null,
    end: Int? = null,
    flags: Int = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
) {
    val startPos = this.length
    append(text)
    setSpan(span, start ?: startPos, end ?: this.length, flags)
}
