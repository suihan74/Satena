package com.suihan74.utilities.bindings

import android.view.View
import androidx.databinding.BindingAdapter
import com.suihan74.utilities.toVisibility

@BindingAdapter("android:visibility")
fun View.setVisibility(isVisible: Boolean?) {
    visibility = isVisible.toVisibility()
}

/**
 * boolean値によってalpha値を操作する
 */
@BindingAdapter(value = ["android:alpha", "disableAlpha"], requireAll = false)
fun View.setAlphaByAvailability(isEnabled: Boolean?, disableAlpha: Float?) {
    alpha = if (isEnabled == true) 1.0f else disableAlpha ?: 0.5f
}
