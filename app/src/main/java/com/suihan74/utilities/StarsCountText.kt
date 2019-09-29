package com.suihan74.utilities

import android.content.Context
import androidx.core.content.ContextCompat

fun appendStarText(builder: StringBuilder, count: Int, context: Context, colorId: Int) {
    val color = ContextCompat.getColor(context, colorId)
    builder.append("<font color=\"$color\">")
    if (count > 10) {
        builder.append("★$count")
    }
    else {
        repeat(count) { builder.append("★") }
    }
    builder.append("</font>")
}
