@file:Suppress("UNUSED")

package com.suihan74.utilities

import android.content.Context

fun Context.px2dp(px: Int) : Float = px2dp(px.toFloat())
fun Context.dp2px(dp: Int) : Int = dp2px(dp.toFloat())
fun Context.px2sp(px: Int) : Float = px2sp(px.toFloat())
fun Context.sp2px(sp: Int) : Int = sp2px(sp.toFloat())

fun Context.px2dp(px: Float) : Float {
    val metrics = resources.displayMetrics
    return px / metrics.density
}

fun Context.dp2px(dp: Float) : Int {
    val metrics = resources.displayMetrics
    return (dp * metrics.density).toInt()
}

fun Context.px2sp(px: Float) : Float {
    val metrics = resources.displayMetrics
    return px / metrics.scaledDensity
}

fun Context.sp2px(sp: Float) : Int {
    val metrics = resources.displayMetrics
    return (sp * metrics.scaledDensity).toInt()
}
