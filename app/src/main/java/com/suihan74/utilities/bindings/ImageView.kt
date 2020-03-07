package com.suihan74.utilities.bindings

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

/** URL先の画像をImageViewで表示 */
@BindingAdapter("src")
fun ImageView.setSource(url: String?) {
    if (url != null) {
        Glide.with(context)
            .load(url)
            .into(this)
    }
}

