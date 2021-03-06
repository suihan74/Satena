package com.suihan74.utilities.extensions

import android.view.Menu
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.view.updateLayoutParams

/** 横幅いっぱいに表示する */
fun SearchView.stretchWidth(menu: Menu) {
    val metrics = resources.displayMetrics

    // SearchView以外のボタンの大きさ (一定である、他は全てただのボタンである前提でベタ書きしているので注意が必要)
    val othersWidth = (menu.size() - 1) * (74 * metrics.density).toInt()

    maxWidth = metrics.widthPixels - othersWidth

    // 左端の余分なマージンを削るための設定
    arrayOf(
        androidx.appcompat.R.id.search_edit_frame,
        androidx.appcompat.R.id.search_mag_icon
    ).forEach { targetId ->
        findViewById<View>(targetId)?.updateLayoutParams<LinearLayout.LayoutParams> {
            marginStart = 0
            leftMargin = 0
        }
    }
}
