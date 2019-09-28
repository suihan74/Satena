package com.suihan74.utilities

import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout

/** タブを長押ししたとき，そのタブのインデックスを引数にして呼ばれる */
fun TabLayout.setOnTabLongClickListener(listener: ((Int)->Boolean)?) {
    val tabs = getChildAt(0) as ViewGroup
    val onLongClickListener = if (listener == null) null else View.OnLongClickListener { v -> listener(tabs.indexOfChild(v)) }

    (0 until tabs.childCount).forEach { idx ->
        tabs.getChildAt(idx)?.setOnLongClickListener(onLongClickListener)
    }
}
