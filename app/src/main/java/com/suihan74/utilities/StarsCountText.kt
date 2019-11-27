package com.suihan74.utilities

import android.content.Context
import androidx.core.content.ContextCompat
import com.suihan74.satena.R

fun appendStarText(builder: StringBuilder, count: Int, context: Context, colorId: Int) {
    val color = ContextCompat.getColor(context, colorId)
    builder.append("<font color=\"$color\">")
    val star = context.getString(R.string.star)
    val starWithCount = context.getString(R.string.star_with_count)
    if (count > 10) {
        builder.append(String.format(starWithCount, count))
    }
    else {
        repeat(count) { builder.append(star) }
    }
    builder.append("</font>")
}
