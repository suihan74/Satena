package com.suihan74.utilities

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * キーボードを表示して対象にフォーカスする
 */
fun Activity.showSoftInputMethod(
    targetView: View,
    softInputMode: Int? = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
) {
    if (softInputMode != null) {
        window?.setSoftInputMode(softInputMode)
    }

    targetView.requestFocus()
    (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.run {
        showSoftInput(targetView, 0)
    }

    if (targetView is EditText) {
        targetView.setSelection(targetView.text.length)
    }
}

/**
 * キーボードを隠して入力対象のビューをアンフォーカスする
 */
fun Activity.hideSoftInputMethod() : Boolean {
    val windowToken = window.decorView.windowToken
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val result = imm?.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    currentFocus?.clearFocus()
    window.decorView.rootView?.requestFocus()
    return result ?: false
}

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
