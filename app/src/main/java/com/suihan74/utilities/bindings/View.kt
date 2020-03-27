package com.suihan74.utilities.bindings

import android.view.View
import androidx.databinding.BindingAdapter
import com.suihan74.utilities.toVisibility

@BindingAdapter("android:visibility")
fun View.setVisibility(isVisible: Boolean?) {
    visibility = isVisible.toVisibility()
}
