package com.suihan74.utilities.bindings

import android.webkit.WebView
import androidx.databinding.BindingAdapter

/**
 * バインドされたURLに遷移する
 */
@BindingAdapter("url")
fun WebView.bindUrl(url: String?) {
    if (this.url != url && url != null) {
        this.stopLoading()
        this.loadUrl(url)
    }
}
