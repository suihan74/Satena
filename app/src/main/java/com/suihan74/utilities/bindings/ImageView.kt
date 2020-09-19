package com.suihan74.utilities.bindings

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Notice
import com.suihan74.utilities.extensions.users

/** URL先の画像をImageViewで表示 */
@BindingAdapter("src")
fun ImageView.setSource(url: String?) {
    if (url != null) {
        Glide.with(context)
            .load(url)
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
