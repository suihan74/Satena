package com.suihan74.utilities.extensions

import android.app.Activity
import android.content.DialogInterface
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog

/**
 * 処理中にダイアログを操作できないようにする
 */
fun AlertDialog.setButtonsEnabled(enabled: Boolean) {
    getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = enabled
    getButton(DialogInterface.BUTTON_NEGATIVE)?.isEnabled = enabled
    getButton(DialogInterface.BUTTON_NEUTRAL)?.isEnabled = enabled
    setCanceledOnTouchOutside(enabled)
    listView?.isEnabled = enabled
}

/**
 * 初期状態でIMEを表示し、画面回転などで閉じないようにする
 */
fun AlertDialog.showSoftInputMethod(activity: Activity, target: View) {
    window?.run {
        clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        )
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
    activity.showSoftInputMethod(
        target,
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
    )
}
