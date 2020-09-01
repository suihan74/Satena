package com.suihan74.utilities.bindings

import android.view.View
import androidx.databinding.BindingAdapter
import com.suihan74.utilities.toVisibility

@BindingAdapter(value = ["android:visibility", "disabledDefaultVisibility"], requireAll = false)
fun View.setVisibility(isVisible: Boolean?, disabledDefault: Int? = View.GONE) {
    // - レイアウト側でdisabledDefaultVisibilityを省略した場合、nullが渡される
    // - 引数を省略した関数呼び出しでの利用も考慮している
    // 以上を満足させるため、引数ではnullableにしてデフォルト値を与えた上で、代入時にもnull比較を行っている
    visibility = isVisible.toVisibility(disabledDefault ?: View.GONE)
}

/**
 * boolean値によってalpha値を操作する
 */
@BindingAdapter(value = ["android:alpha", "disableAlpha"], requireAll = false)
fun View.setAlphaByAvailability(isEnabled: Boolean?, disableAlpha: Float?) {
    alpha = if (isEnabled == true) 1.0f else disableAlpha ?: 0.5f
}
