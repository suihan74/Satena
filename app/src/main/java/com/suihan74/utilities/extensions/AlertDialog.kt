package com.suihan74.utilities.extensions

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope

/**
 * 処理中にダイアログを操作できないようにする
 */
@MainThread
fun AlertDialog.setButtonsEnabled(enabled: Boolean) {
    runCatching {
        getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = enabled
        getButton(DialogInterface.BUTTON_NEGATIVE)?.isEnabled = enabled
        getButton(DialogInterface.BUTTON_NEUTRAL)?.isEnabled = enabled
        setCanceledOnTouchOutside(enabled)
        listView?.isEnabled = enabled
    }
}

/**
 * 初期状態でIMEを表示し、画面回転などで閉じないようにする
 */
@MainThread
fun Dialog.showSoftInputMethod(activity: Activity, target: View) {
    runCatching {
        window?.run {
            clearFlags(FLAG_NOT_FOCUSABLE or FLAG_ALT_FOCUSABLE_IM)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        activity.showSoftInputMethod(
            target,
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
    }
}

/**
 * 初期状態でIMEを表示し、画面回転などで閉じないようにする
 */
fun DialogFragment.showSoftInputMethod(target: View) = lifecycleScope.launchWhenResumed {
    runCatching {
        dialog?.window?.run {
            clearFlags(FLAG_NOT_FOCUSABLE or FLAG_ALT_FOCUSABLE_IM)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        activity?.showSoftInputMethod(
            target,
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
    }
}
