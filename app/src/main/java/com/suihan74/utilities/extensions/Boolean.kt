@file:Suppress("unused")

package com.suihan74.utilities.extensions

import android.view.View

/** trueのときView.VISIBLEにする */
fun Boolean?.toVisibility(defaultInvisible: Int = View.GONE) : Int =
    if (this == true) View.VISIBLE
    else defaultInvisible

/** trueのときに処理を実行する */
inline fun Boolean?.whenTrue(crossinline action: ()->Unit) : Boolean {
    val result = this == true
    if (result) action.invoke()
    return result
}

/** falseのときに処理を実行する */
inline fun Boolean?.whenFalse(crossinline action: ()->Unit) : Boolean {
    val result = this == true
    if (!result) action.invoke()
    return result
}
