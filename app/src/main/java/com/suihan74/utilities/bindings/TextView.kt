package com.suihan74.utilities.bindings

import android.net.Uri
import android.widget.TextView
import androidx.databinding.BindingAdapter

/** ドメイン表示 */
@BindingAdapter("rootUrl", "url")
fun TextView.setDomain(rootUrl: String, url: String) {
    val rootUrlRegex = Regex("""https?://(.+)/$""")
    text = rootUrlRegex.find(rootUrl)?.groupValues?.get(1) ?: Uri.parse(url).host
}
