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
