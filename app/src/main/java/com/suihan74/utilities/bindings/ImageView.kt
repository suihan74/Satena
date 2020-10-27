package com.suihan74.utilities.bindings

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.GlideApp
import com.suihan74.utilities.extensions.users

/** URL先の画像をImageViewで表示 */
@BindingAdapter(value = ["src", "errorSrc"], requireAll = false)
fun ImageView.setSource(url: String?, errorSrc: Drawable? = null) {
    val context = context ?: return
    if (url.isNullOrBlank()) {
        if (errorSrc == null) {
            setImageResource(android.R.color.transparent)
        }
        else {
            setImageDrawable(errorSrc)
        }
    }
    else {
        GlideApp.with(context)
            .load(url)
            .error(errorSrc)
            .into(this)
    }
}

/** 通知アイテムに表示するアイコン */
@BindingAdapter("noticeImage")
fun ImageView.setNoticeImage(notice: Notice?) {
    notice?.users?.firstOrNull()?.let { user ->
        setSource(HatenaClient.getUserIconUrl(user))
    }
}
