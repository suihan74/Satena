package com.suihan74.utilities.bindings

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.suihan74.hatenaLib.Notice
import com.suihan74.hatenaLib.Star
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.utilities.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

/** 左付き画像 */
@BindingAdapter("drawableLeft")
fun TextView.setDrawableLeft(url: String) {
    Glide.with(context)
        .load(url)
        .into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                setCompoundDrawablesWithIntrinsicBounds(resource, null, null, null)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }
        })
}

/** ドメイン表示 */
@BindingAdapter("rootUrl", "url")
fun TextView.setDomain(rootUrl: String, url: String) {
    val rootUrlRegex = Regex("""https?://(.+)/$""")
    text = rootUrlRegex.find(rootUrl)?.groupValues?.get(1) ?: Uri.parse(url).host
}

/** タイムスタンプ */
@BindingAdapter("timestamp")
fun TextView.setTimestamp(timestamp: LocalDateTime?) {
    if (timestamp != null) {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        text = timestamp.format(formatter)
    }
}

/** エントリリストでのブコメユーザー名表示 */
@BindingAdapter("user", "private")
fun TextView.setBookmarkResultUser(user: String?, private: Boolean?) {
    text = user
    if (private == true) {
        val icon = resources.getDrawable(R.drawable.ic_baseline_lock, null).apply {
            val size = textSize.toInt()
            setBounds(0, 0, size, size)
            setTint(context.getThemeColor(R.attr.textColor))
        }
        compoundDrawablePadding = 4
        setCompoundDrawablesRelative(null, null, icon, null)
    }
}

/** エントリリストでのブコメ表示 */
@BindingAdapter("comment", "starsCount")
fun TextView.setBookmarkResult(comment: String?, starsCount: List<Star>?) {
    val commentBuilder = StringBuilder(comment ?: "")
    if (starsCount != null) {
        val yellowStarCount = starsCount.firstOrNull { it.color == StarColor.Yellow }?.count ?: 0
        val redStarCount = starsCount.firstOrNull { it.color == StarColor.Red }?.count ?: 0
        val greenStarCount = starsCount.firstOrNull { it.color == StarColor.Green }?.count ?: 0
        val blueStarCount = starsCount.firstOrNull { it.color == StarColor.Blue }?.count ?: 0
        val purpleStarCount = starsCount.firstOrNull { it.color == StarColor.Purple }?.count ?: 0

        commentBuilder.append(" ")
        appendStarText(commentBuilder, purpleStarCount, context, R.color.starPurple)
        appendStarText(commentBuilder, blueStarCount, context, R.color.starBlue)
        appendStarText(commentBuilder, redStarCount, context, R.color.starRed)
        appendStarText(commentBuilder, greenStarCount, context, R.color.starGreen)
        appendStarText(commentBuilder, yellowStarCount, context, R.color.starYellow)
    }

    setHtml(commentBuilder.toString())
    visibility = (!comment.isNullOrBlank()).toVisibility()
}

/** 通知項目のテキスト */
@BindingAdapter("noticeText")
fun TextView.setNoticeText(notice: Notice) {
    setHtml(notice.message(context))
}