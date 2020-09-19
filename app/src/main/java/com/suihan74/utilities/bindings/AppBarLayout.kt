package com.suihan74.utilities.bindings

import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import com.google.android.material.appbar.AppBarLayout

@BindingAdapter("android:visibility")
fun AppBarLayout.setVisibility(isVisible: Boolean?) {
    // visibility = isVisible.toVisibility(disabledDefault ?: View.GONE)
    // AppBarLayoutの場合GONEにすると領域が残る
    // 高さを0にすることで代替する
    updateLayoutParams<CoordinatorLayout.LayoutParams> {
        height =
            if (isVisible == true) CoordinatorLayout.LayoutParams.WRAP_CONTENT
            else 0
    }
}
