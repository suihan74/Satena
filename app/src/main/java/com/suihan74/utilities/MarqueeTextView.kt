package com.suihan74.utilities

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/** Marqueeを正しく動作させるためのTextView */
class MarqueeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleId: Int = 0
) : AppCompatTextView(context, attrs, defStyleId) {

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (focused) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus)
        }
    }

    override fun isFocused(): Boolean = true
}
