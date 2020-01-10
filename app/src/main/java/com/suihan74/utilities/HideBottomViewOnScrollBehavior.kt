package com.suihan74.utilities

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior

class HideBottomViewOnScrollBehavior<V : View>(
    context: Context?,
    attrs: AttributeSet?
) : HideBottomViewOnScrollBehavior<V>(context, attrs) {

    var hidden = false
        private set

    public override fun slideDown(child: V) {
        if (!hidden) {
            super.slideDown(child)
        }
        hidden = true
    }

    public override fun slideUp(child: V) {
        if (hidden) {
            super.slideUp(child)
        }
        hidden = false
    }
}
