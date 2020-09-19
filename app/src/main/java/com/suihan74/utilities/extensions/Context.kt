@file:Suppress("UNUSED")

package com.suihan74.utilities.extensions

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.Toast
import com.suihan74.satena.R

// ------ //

fun Context.showToast(message: String) {
    val dimen = this.resources.getDimension(R.dimen.toast_offset_y)
    val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT).apply {
        setGravity(Gravity.TOP, 0, dimen.toInt())
    }
    toast.show()
}

fun Context.showToast(messageId: Int) =
    showToast(getString(messageId))

fun Context.showToast(messageId: Int, vararg args: Any) =
    showToast(getString(messageId, *args))

// ------ //

/** pxをdpに変換 */
fun Context.px2dp(px: Int) : Float = px2dp(px.toFloat())

/** pxをdpに変換 */
fun Context.px2dp(px: Float) : Float {
    val metrics = resources.displayMetrics
    return px / metrics.density
}

// ------ //

/** dpをpxに変換 */
fun Context.dp2px(dp: Int) : Int = dp2px(dp.toFloat())

/** dpをpxに変換 */
fun Context.dp2px(dp: Float) : Int {
    val metrics = resources.displayMetrics
    return (dp * metrics.density).toInt()
}

// ------ //

/** pxをspに変換 */
fun Context.px2sp(px: Int) : Float = px2sp(px.toFloat())

/** pxをspに変換 */
fun Context.px2sp(px: Float) : Float {
    val metrics = resources.displayMetrics
    return px / metrics.scaledDensity
}

// ------ //

/** spをpxに変換 */
fun Context.sp2px(sp: Int) : Int = sp2px(sp.toFloat())

/** spをpxに変換 */
fun Context.sp2px(sp: Float) : Int {
    val metrics = resources.displayMetrics
    return (sp * metrics.scaledDensity).toInt()
}

// ------ //

/**
 * テーマに設定された色を取得する
 */
fun Context.getThemeColor(attrId: Int) : Int {
    val outValue = TypedValue()
    theme.resolveAttribute(attrId, outValue, true)
    return outValue.data
}
