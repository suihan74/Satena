package com.suihan74.utilities

import android.app.Activity
import android.view.Menu
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.view.updateLayoutParams

/** 横幅いっぱいに表示する */
fun SearchView.stretchWidth(activity: Activity, menu: Menu, isBottomAppBar: Boolean = false) {
    val dMetrics = android.util.DisplayMetrics()
    activity.windowManager.defaultDisplay.getMetrics(dMetrics)

    // SearchView以外のボタンの大きさ (一定である、他は全てただのボタンである前提でベタ書きしているので注意が必要)
    val othersWidth = (menu.size() - 1) * (66 * resources.displayMetrics.density).toInt()

    // ボトムバーに表示するときはFAB部分を回避する
    val rightMargin = if (isBottomAppBar) (48 * resources.displayMetrics.density).toInt() else 0

    maxWidth = dMetrics.widthPixels - othersWidth - rightMargin

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
