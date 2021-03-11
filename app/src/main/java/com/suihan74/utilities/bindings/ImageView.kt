package com.suihan74.utilities.bindings

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.GlideApp
import com.suihan74.utilities.extensions.users

object ImageViewBindingAdapters {
    @JvmStatic
    @BindingAdapter("android:src", "tint")
    fun setSource(imageView: ImageView, @DrawableRes drawableId: Int?, tint: ColorStateList?) {
        drawableId?.let {
            imageView.setImageResource(it)
        }
        ImageViewCompat.setImageTintList(imageView, tint)
    }

    /** URL先の画像をImageViewで表示 */
    @JvmStatic
    @BindingAdapter(value = ["src", "errorSrc"], requireAll = false)
    fun setSource(imageView: ImageView, url: String?, errorSrc: Drawable? = null) {
        val context = imageView.context ?: return
        if (url.isNullOrBlank()) {
            if (errorSrc == null) {
                imageView.setImageResource(android.R.color.transparent)
            }
            else {
                imageView.setImageDrawable(errorSrc)
            }
        }
        else {
            GlideApp.with(context)
                .load(url)
                .error(errorSrc)
                .into(imageView)
        }
    }

    @JvmStatic
    @BindingAdapter("src")
    fun setSource(imageView: ImageView, bitmap: Bitmap?) {
        GlideApp.with(imageView.context)
            .load(bitmap)
            .into(imageView)
    }

    /** 通知アイテムに表示するアイコン */
    @JvmStatic
    @BindingAdapter("noticeImage")
    fun setNoticeImage(imageView: ImageView, notice: Notice?) {
        notice?.users?.firstOrNull()?.let { user ->
            setSource(imageView, HatenaClient.getUserIconUrl(user))
        }
    }

}
