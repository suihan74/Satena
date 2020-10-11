package com.suihan74.utilities.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter

/** Marqueeを正しく動作させるためのTextView */
class MarqueeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleId: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleId) {

    companion object {
        @JvmStatic
        @BindingAdapter("marqueeEnabled")
        fun setMarqueeEnabled(view: MarqueeTextView, b: Boolean) {
            view.marqueeEnabled = b
            view.isSelected = b
        }
    }

    var marqueeEnabled: Boolean = true

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (!marqueeEnabled || focused) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!marqueeEnabled || hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus)
        }
    }

    override fun isFocused(): Boolean = marqueeEnabled
}
