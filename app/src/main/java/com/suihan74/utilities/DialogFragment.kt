package com.suihan74.utilities

import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/** エラー処理を省略しても失敗時にクラッシュしないDialogFragment#show() */
fun DialogFragment.showAllowingStateLoss(
    fragmentManager: FragmentManager,
    tag: String? = null,
    onError: OnError? = { Log.e("DialogFragment", Log.getStackTraceString(it)) }
) {
    try {
        show(fragmentManager, tag)
    }
    catch (e: Throwable) {
        onError?.invoke(e)
    }
}
