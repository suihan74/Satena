package com.suihan74.utilities.bindings

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.suihan74.hatenaLib.MaintenanceEntry
import com.suihan74.hatenaLib.Notice
import com.suihan74.hatenaLib.Star
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.utilities.extensions.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

/** フォントサイズを指定(sp), バインディング用 */
@BindingAdapter("textSizeSp")
fun TextView.textSizeSp(size: Float) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
}

//////////////////////////////////////////////////

/**
 * 数値をテキストとしてセットする
 *
 * 数値を直接渡す際にリソースIDとして認識されないようにするために使用
 */
@BindingAdapter("numText")
fun TextView.setNumberText(value: Number?) {
    this.text = value?.toString().orEmpty()
}

//////////////////////////////////////////////////

class TextViewWithDrawable @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleInt: Int = 0
) : AppCompatTextView(context, attrs, defStyleInt)

object TexViewWithDrawableBinder {
    /** drawableをテキストと同じサイズにして追加する */
    @JvmStatic
    @BindingAdapter(
        value = ["drawableStart", "drawableTop", "drawableEnd", "drawableBottom"],
        requireAll = false
    )
    fun TextViewWithDrawable.setBoundedDrawable(
        drawableStart: Drawable? = null,
        drawableTop: Drawable? = null,
        drawableEnd: Drawable? = null,
        drawableBottom: Drawable? = null,
    ) {
        val color = currentTextColor

        val start = drawableStart?.setTextSizeBounds(this, color)
        val end = drawableEnd?.setTextSizeBounds(this, color)
        val top = drawableTop?.setTextSizeBounds(this, color)
        val bottom = drawableBottom?.setTextSizeBounds(this, color)

        setCompoundDrawables(start, top, end, bottom)
    }

    private fun Drawable.setTextSizeBounds(
        textView: TextView,
        color: Int
    ) : Drawable = apply {
        val size = textView.textSize.toInt()
        setBounds(0, 0, size, size)
        setTint(color)
    }
}

//////////////////////////////////////////////////

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
fun TextView.setDomain(rootUrl: String?, url: String?) {
    if (rootUrl == null || url == null) return

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

//////////////////////////////////////////////////

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
        commentBuilder.appendStarText(purpleStarCount, context, R.color.starPurple)
        commentBuilder.appendStarText(blueStarCount, context, R.color.starBlue)
        commentBuilder.appendStarText(redStarCount, context, R.color.starRed)
        commentBuilder.appendStarText(greenStarCount, context, R.color.starGreen)
        commentBuilder.appendStarText(yellowStarCount, context, R.color.starYellow)
    }

    setHtml(commentBuilder.toString())
    visibility = (!comment.isNullOrBlank()).toVisibility()
}

//////////////////////////////////////////////////

/** 通知項目のテキスト */
@BindingAdapter("noticeText")
fun TextView.setNoticeText(notice: Notice) {
    setHtml(notice.message(context))
}

//////////////////////////////////////////////////

/** 障害情報のタイトルテキスト装飾 */
@BindingAdapter("maintenanceTitle")
fun TextView.setMaintenanceTitle(title: String) {
    val resolvedColor = ContextCompat.getColor(context, R.color.maintenanceResolved)
    setHtml(title.replace("【復旧済み】", "<font color=\"$resolvedColor\">【復旧済み】</font>"))
}


/** 障害情報のタイトルテキスト装飾 */
@BindingAdapter("html")
fun TextView.setHtmlText(title: String) {
    setHtml(title)
}

/** 障害情報のタイムスタンプ */
@BindingAdapter("maintenanceTimestamp")
fun TextView.setMaintenanceTimestamp(value: MaintenanceEntry) {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    text =
        if (value.timestamp == value.timestampUpdated) {
            value.timestamp.format(dateTimeFormatter)
        }
        else {
            buildString {
                append(value.timestamp.format(dateTimeFormatter))
                append("  (更新: ", value.timestampUpdated.format(dateTimeFormatter), ")")
            }
        }
}
