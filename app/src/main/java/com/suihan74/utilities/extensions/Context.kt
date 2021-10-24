@file:Suppress("UNUSED")

package com.suihan74.utilities.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// ------ //

/**
 * 同種のトースト通知の重複を防ぐためのタグ
 *
 * ex)
 *
 * enum class HogeToastTag : ToastTag {
 *     MSG_FOO
 * }
 */
interface ToastTag

object ContextExtensions {
    private val liveTags = HashSet<ToastTag>()
    private val liveTagsMutex = Mutex()

    fun Context.showToast(message: String) {
        runCatching {
            val dimen = this.resources.getDimension(R.dimen.toast_offset_y)
            val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT).apply {
                setGravity(Gravity.TOP, 0, dimen.toInt())
            }
            toast.show()
        }
    }

    fun Context.showToast(messageId: Int) = showToast(getString(messageId))

    fun Context.showToast(messageId: Int, vararg args: Any) = showToast(getString(messageId, *args))

    /**
     * 同一タグがついたトーストの重複通知を回避する
     */
    fun Context.showToast(message: String, tag: ToastTag) = SatenaApplication.instance.coroutineScope.launch {
        liveTagsMutex.withLock {
            if (liveTags.contains(tag)) return@launch
            liveTags.add(tag)
        }
        withContext(Dispatchers.Main) { showToast(message) }
        delay(2_000L)
        liveTagsMutex.withLock {
            liveTags.remove(tag)
        }
    }

    fun Context.showToast(messageId: Int, tag: ToastTag) = showToast(getString(messageId), tag)

    fun Context.showToast(messageId: Int, tag: ToastTag, vararg args: Any) = showToast(getString(messageId, *args), tag)

    // --- //

    fun Fragment.showToast(message: String) = requireContext().showToast(message)

    fun Fragment.showToast(messageId: Int) = requireContext().showToast(messageId)

    fun Fragment.showToast(messageId: Int, vararg args: Any) = requireContext().showToast(messageId, *args)

    fun Fragment.showToast(message: String, tag: ToastTag) = requireContext().showToast(message, tag)

    fun Fragment.showToast(messageId: Int, tag: ToastTag) = requireContext().showToast(messageId, tag)

    fun Fragment.showToast(messageId: Int, tag: ToastTag, vararg args: Any) = requireContext().showToast(messageId, tag, *args)
}

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
fun Context.getThemeColor(@AttrRes attrId: Int) : Int =
    with(TypedValue()) {
        theme.resolveAttribute(attrId, this, true)
        data
    }

/**
 * テーマに設定された`Drawable`リソースIDを取得する
 */
fun Context.getThemeDrawableId(@AttrRes attrId: Int) : Int =
    with(TypedValue()) {
        theme.resolveAttribute(attrId, this, true)
        resourceId
    }

/**
 * テーマに設定された`Drawable`リソースを取得する
 */
fun Context.getThemeDrawable(@AttrRes attrId: Int) : Drawable? =
    ContextCompat.getDrawable(this, getThemeDrawableId(attrId))
