package com.suihan74.utilities.bindings

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow

/**
 * サイズを表す数値を表示する
 *
 * @param size: 表示する数値
 * @param unit: 数値の単位
 */
@BindingAdapter(value = ["sizeText", "unit"], requireAll = false)
fun TextView.setSizeText(size: Long?, unit: String? = "") {
    if (size == null) {
        this.text = ""
        return
    }

    val rawSize = kotlin.math.max(0L, size)

    val metrics = arrayOf("", "Ki", "Mi", "Gi", "Ti")
    val exp = kotlin.math.min(
        if (rawSize == 0L) 0
        else floor(log(rawSize.toDouble(), 1024.0)).toInt(),
        metrics.lastIndex
    )
    val metric = metrics[exp]
    val num = rawSize / 1024.0.pow(exp)

    this.text = String.format("%.1f%s%s", num, metric, unit.orEmpty())
}

//////////////////////////////////////////////////

/**
 * 画面の向きごとに最大表示行数を設定する
 */
@BindingAdapter("maxLinesPortrait", "maxLinesLandscape")
fun TextView.setMaxLinesWithScreenRotation(
    maxLinesPortrait: Int,
    maxLinesLandscape: Int
) {
    val displayMetrics = this.context.resources.displayMetrics
    this.maxLines =
        if (displayMetrics.widthPixels > displayMetrics.heightPixels) maxLinesLandscape
        else maxLinesPortrait
}

//////////////////////////////////////////////////

/** フォントサイズを指定(sp), バインディング用 */
@BindingAdapter("textSizeSp")
fun TextView.textSizeSp(size: Float) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
}

//////////////////////////////////////////////////

/** エンコードされたURLをデコードして表示する */
@BindingAdapter("encodedUrl")
fun TextView.setUrlWithDecoding(encodedUrl: String?) {
    text = encodedUrl?.let { Uri.decode(it) } ?: ""
}

//////////////////////////////////////////////////

object TextViewBindingAdapters {
    /**
     * 0x0リソースを回避して文字列リソースをセットする
     */
    @JvmStatic
    @BindingAdapter("android:text")
    fun bindTextResource(textView: TextView, textId: Int?) {
        textView.text =
            if (textId == null || textId == 0) ""
            else textView.context.getText(textId)
    }

    /**
     * 数値をテキストとしてセットする
     *
     * 数値を直接渡す際にリソースIDとして認識されないようにするために使用
     */
    @JvmStatic
    @BindingAdapter("numText")
    fun bindNumberText(textView: TextView, value: Number?) {
        textView.text = value?.toString().orEmpty()
    }
}

//////////////////////////////////////////////////

class TextViewWithDrawable @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleInt: Int = 0
) : AppCompatTextView(context, attrs, defStyleInt)

object TextViewWithDrawableBinder {
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

    /** drawableをテキストと同じサイズにして追加する */
    @JvmStatic
    @BindingAdapter(
        value = ["drawableStart", "drawableTop", "drawableEnd", "drawableBottom"],
        requireAll = false
    )
    fun TextViewWithDrawable.setBoundedDrawable(
        @DrawableRes drawableStartId: Int? = null,
        @DrawableRes drawableTopId: Int? = null,
        @DrawableRes drawableEndId: Int? = null,
        @DrawableRes drawableBottomId: Int? = null,
    ) {
        val drawableStart = drawableStartId?.let { ContextCompat.getDrawable(context, it) }
        val drawableTop = drawableTopId?.let { ContextCompat.getDrawable(context, it) }
        val drawableEnd = drawableEndId?.let { ContextCompat.getDrawable(context, it) }
        val drawableBottom = drawableBottomId?.let { ContextCompat.getDrawable(context, it) }
        setBoundedDrawable(drawableStart, drawableTop, drawableEnd, drawableBottom)
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
@BindingAdapter(value = ["timestamp", "timezone"], requireAll = false)
fun TextView.setTimestamp(timestamp: LocalDateTime?, timezone: String?) {
    text = timestamp
        ?.toSystemZonedDateTime(timezone ?: "Asia/Tokyo")
        ?.format(DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm"))
        .orEmpty()
}

//////////////////////////////////////////////////

/** エントリリストでのブコメユーザー名表示 */
@BindingAdapter("user", "private")
fun TextView.setBookmarkResultUser(user: String?, private: Boolean?) {
    text = user
    if (private == true) {
        val icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_lock, null)?.apply {
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
    val dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss")
    val zone = "Asia/Tokyo"
    text =
        if (value.timestamp == value.timestampUpdated) {
            value.timestamp.toSystemZonedDateTime(zone).format(dateTimeFormatter)
        }
        else {
            buildString {
                append(value.timestamp.toSystemZonedDateTime(zone).format(dateTimeFormatter))
                append("  (更新: ", value.timestampUpdated.toSystemZonedDateTime(zone).format(dateTimeFormatter), ")")
            }
        }
}
