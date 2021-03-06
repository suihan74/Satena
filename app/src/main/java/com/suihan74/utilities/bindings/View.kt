package com.suihan74.utilities.bindings

import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.databinding.BindingAdapter
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import com.suihan74.utilities.extensions.toVisibility

@BindingAdapter(
    value = ["android:visibility", "disabledDefaultVisibility"],
    requireAll = false
)
fun View.setVisibility(
    isVisible: Boolean?,
    disabledDefault: Int? = View.GONE
) {
    // こちらのアダプタの方が優先して呼ばれてしまうので、
    // ここで明示的にAppBarLayout用のアダプタを呼ぶ
    if (this is AppBarLayout) {
        return AppBarLayoutBindingAdapters.setVisibility(this, isVisible)
    }

    // - レイアウト側でdisabledDefaultVisibilityを省略した場合、nullが渡される
    // - 引数を省略した関数呼び出しでの利用も考慮している
    // 以上を満足させるため、引数ではnullableにしてデフォルト値を与えた上で、代入時にもnull比較を行っている
    visibility = isVisible.toVisibility(disabledDefault ?: View.GONE)
}

@BindingAdapter(
    value = ["android:visibility", "disabledDefaultVisibility", "transition", "container"],
    requireAll = false
)
fun View.setVisibilityWithTransition(
    isVisible: Boolean?,
    disabledDefault: Int? = View.GONE,
    transition: Transition? = null,
    container: ViewGroup? = null
) {
    if (transition != null && container != null) {
        TransitionManager.beginDelayedTransition(
            container,
            transition
        )
    }
    setVisibility(isVisible, disabledDefault)
}

// ------ //

object ViewBindingAdapters {
    /**
     * boolean値によってalpha値を操作する
     */
    @JvmStatic
    @BindingAdapter(value = ["android:alpha", "disableAlpha"], requireAll = false)
    fun setAlphaByAvailability(view: View, isEnabled: Boolean?, disableAlpha: Float?) {
        view.alpha =
            if (isEnabled == true) 1.0f
            else disableAlpha ?: 0.5f
    }

    /**
     * ツールチップテキストを設定する
     */
    @JvmStatic
    @BindingAdapter(value = ["tooltipText"])
    fun setTooltipText(view: View, text: CharSequence?) {
        TooltipCompat.setTooltipText(view, text)
    }

    /**
     * ツールチップテキストを設定する
     */
    @JvmStatic
    @BindingAdapter(value = ["tooltipText"])
    fun setTooltipText(view: View, @StringRes textId: Int?) {
        TooltipCompat.setTooltipText(
            view,
            textId?.let { view.context.getText(it) }
        )
    }
}

/**
 * 空文字列のとき非表示にする
 */
@BindingAdapter(value = ["android:visibility", "disabledDefaultVisibility"], requireAll = false)
fun View.setVisibility(charSequence: CharSequence?, disabledDefault: Int? = View.GONE) {
    setVisibility(!charSequence.isNullOrBlank(), disabledDefault)
}
