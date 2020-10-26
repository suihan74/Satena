package com.suihan74.utilities.extensions

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.suihan74.satena.BuildConfig
import java.io.File

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
}

/**
 * キーボードを隠して入力対象のビューをアンフォーカスする
 */
fun Activity.hideSoftInputMethod(focusTarget: View? = null) : Boolean {
    val windowToken = window.decorView.windowToken
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val result = imm?.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    currentFocus?.clearFocus()

    (focusTarget ?: window.decorView.rootView)?.let { target ->
        target.isFocusable = true
        target.isFocusableInTouchMode = true
        target.requestFocus()
    }

    return result ?: false
}

// ------ //

/**
 * ```FileProvider``を利用して外部アプリに共有できるファイルURIを取得する
 *
 * @param file ```AndroidManifest.xml```に公開可能として登録済みのディレクトリに置かれたファイル
 */
fun Activity.getSharableFileUri(file: File) : Uri {
    return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file)
}

// ------ //

/**
 * アクティビティで表示中のフラグメントを探す
 */
@Suppress("UNCHECKED_CAST")
fun <T : Fragment> AppCompatActivity.findFragmentByTag(
    tag: String,
    fragmentManager: FragmentManager = supportFragmentManager
) =
    fragmentManager.findFragmentByTag(tag) as? T
