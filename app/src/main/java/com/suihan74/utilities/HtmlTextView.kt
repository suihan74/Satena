package com.suihan74.utilities

import android.os.Build
import android.text.Html
import android.widget.TextView

@Suppress("deprecation")
fun makeSpannedfromHtml(html: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    }
    else {
        Html.fromHtml(html)
    }

fun TextView.setHtml(html: String) {
    this.text = makeSpannedfromHtml(html)
}
