package com.suihan74.utilities.bindings

import android.util.Log
import androidx.databinding.BindingAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

/** FABアイコンをリソースで指定 */
@BindingAdapter("src")
fun FloatingActionButton.setIconId(resId: Int?) {
    if (resId == null) return

    try {
        setImageResource(resId)
    }
    catch (e: Throwable) {
        Log.e("resource error", Log.getStackTraceString(e))
    }
}

/** FABの表示状態を真偽値で指定 */
@BindingAdapter("isShown")
fun FloatingActionButton.setShown(flag: Boolean) {
    if (flag) this.show()
    else this.hide()
}
