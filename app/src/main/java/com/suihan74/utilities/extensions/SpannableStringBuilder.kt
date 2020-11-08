package com.suihan74.utilities.extensions

import android.content.Context
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * 装飾を施したテキストを追加する
 *
 * append(text, span)は以下のコードと等価
 *
 * val start = builder.length
 *
 * builder.append(text)
 *
 * builder.setSpan(span, start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
 */
fun SpannableStringBuilder.append(
    text: String,
    span: Any,
    start: Int? = null,
    end: Int? = null,
    flags: Int = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
) {
    val startPos = start ?: this.length
    val endPos = end ?: (startPos + text.length)

    if (startPos < endPos) {
        append(text)
        setSpan(span, startPos, endPos, flags)
    }
}

/**
 * ```SpannableStringBuilder```に画像リソースを追加する
 *
 * @param verticalAlign 縦方向の位置指定 (省略時、(可能な限り)中央揃え)
 * - [android.text.style.DynamicDrawableSpan#ALIGN_BASELINE]
 * - [android.text.style.DynamicDrawableSpan#ALIGN_BOTTOM]
 * - [android.text.style.DynamicDrawableSpan#ALIGN_CENTER]
 */
fun SpannableStringBuilder.appendDrawable(
    context: Context,
    @DrawableRes resId: Int,
    sizePx: Int,
    color: Int,
    verticalAlign: Int? = null
) {
    ContextCompat.getDrawable(context, resId)?.let { drawable ->
        drawable.setTint(color)
        drawable.setBounds(0, 0, sizePx, sizePx)

        val vAlign = verticalAlign ?:
            if (Build.VERSION.SDK_INT >= 29) ImageSpan.ALIGN_CENTER
            else ImageSpan.ALIGN_BASELINE

        append("_", ImageSpan(drawable, vAlign))
    }
}

/**
 * ```SpannableStringBuilder```に画像リソースを追加する
 *
 * @param verticalAlign 縦方向の位置指定 (省略時、(可能な限り)中央揃え)
 * - [android.text.style.DynamicDrawableSpan#ALIGN_BASELINE]
 * - [android.text.style.DynamicDrawableSpan#ALIGN_BOTTOM]
 * - [android.text.style.DynamicDrawableSpan#ALIGN_CENTER]
 */
fun SpannableStringBuilder.appendDrawable(
    textView: TextView,
    @DrawableRes resId: Int,
    sizePx: Int? = null,
    color: Int? = null,
    verticalAlign: Int? = null
) {
    appendDrawable(
        context = textView.context,
        resId = resId,
        sizePx = sizePx ?: textView.lineHeight,
        color = color ?: textView.textColors.defaultColor,
        verticalAlign = verticalAlign
    )
}
