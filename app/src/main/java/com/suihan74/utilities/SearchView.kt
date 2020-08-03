package com.suihan74.utilities

import android.app.Activity
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.view.updateLayoutParams

/** 横幅いっぱいに表示する */
fun SearchView.stretchWidth(activity: Activity) {
    val dMetrics = android.util.DisplayMetrics()
    activity.windowManager.defaultDisplay.getMetrics(dMetrics)
    val buttonSize = (64 * resources.displayMetrics.density).toInt()  // TEXT/TAGボタンのサイズ分だけ小さくしないとボタンが画面外に出てしまう
    maxWidth = dMetrics.widthPixels - buttonSize

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
