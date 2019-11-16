package com.suihan74.utilities

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager

/**
 * キーボードを隠して入力対象のビューをアンフォーカスする
 */
fun Activity.hideSoftInputMethod() = currentFocus?.let { focusedView ->
    focusedView.clearFocus()
    val im = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    im?.hideSoftInputFromWindow(
        focusedView.windowToken,
        InputMethodManager.HIDE_NOT_ALWAYS
    )
} ?: false


fun Context.getThemeColor(attrId: Int) : Int {
    val outValue = TypedValue()
    theme.resolveAttribute(attrId, outValue, true)
    return outValue.data
}
