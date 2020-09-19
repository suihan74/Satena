package com.suihan74.utilities.bindings

import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import com.google.android.material.appbar.AppBarLayout

// ViewのBindingAdapterと重複するため、objectで用意する
object AppBarLayoutBindingAdapters {
    @JvmStatic
    @BindingAdapter("android:visibility")
    fun setVisibility(view: AppBarLayout, isVisible: Boolean?) {
        // AppBarLayoutの場合GONEにすると領域が残る
        // 高さを0にすることで代替する
        view.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            height =
                if (isVisible == true) CoordinatorLayout.LayoutParams.WRAP_CONTENT
                else 0
        }
    }
}
