package com.suihan74.utilities

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * キーボードを表示して対象にフォーカスする
 */
fun Activity.showSoftInputMethod(targetView: View) {
    (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.run {
        toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.SHOW_IMPLICIT)
    }
    targetView.requestFocus()
}

/**
 * キーボードを隠して入力対象のビューをアンフォーカスする
 */
fun Activity.hideSoftInputMethod() = currentFocus?.let { focusedView ->
    focusedView.clearFocus()
    (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.run {
        hideSoftInputFromWindow(focusedView.windowToken, 0)
    }
} ?: false

/**
 * テーマに設定された色を取得する
 */
fun Context.getThemeColor(attrId: Int) : Int {
    val outValue = TypedValue()
    theme.resolveAttribute(attrId, outValue, true)
    return outValue.data
}

/**
 * アクティビティで表示中のフラグメントを探す
 */
@Suppress("UNCHECKED_CAST")
fun <T : Fragment> AppCompatActivity.findFragmentByTag(
    tag: String,
    fragmentManager: FragmentManager = supportFragmentManager
) =
    fragmentManager.findFragmentByTag(tag) as? T
