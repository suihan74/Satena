package com.suihan74.utilities.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.children

class TouchInterceptableMotionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleId: Int = 0
) : MotionLayout(context, attrs, defStyleId) {
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false
        val (x, y) = event.x.toInt() to event.y.toInt()

        val childrenTouched =
            children.any {
                val rect = Rect()
                it.getHitRect(rect)
                rect.contains(x, y)
            }

        if (childrenTouched) {
            return super.onTouchEvent(event)
        }
        else {
            return false
        }
    }
}
