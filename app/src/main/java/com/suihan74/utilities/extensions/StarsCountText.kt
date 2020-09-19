package com.suihan74.utilities.extensions

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import androidx.core.content.ContextCompat
import com.suihan74.satena.R

fun StringBuilder.appendStarText(count: Int, context: Context, colorId: Int) {
    val color = ContextCompat.getColor(context, colorId)
    this.append("<font color=\"$color\">")
    val star = context.getString(R.string.star)
    val starWithCount = context.getString(R.string.star_with_count)
    if (count > 10) {
        this.append(String.format(starWithCount, count))
    }
    else {
        repeat(count) { this.append(star) }
    }
    this.append("</font>")
}

/**
 * @return 追加した文字列の長さ
 */
fun SpannableStringBuilder.appendStarSpan(count: Int, context: Context, spanStyleId: Int): Int {
    val text =
        if (count > 10) context.getString(R.string.star_with_count, count)
        else buildString {
            val star = context.getString(R.string.star)
            repeat(count) { append(star) }
        }

    val colorSpan = TextAppearanceSpan(context, spanStyleId)

    this.append(text, colorSpan)

    return text.length
}
