package com.suihan74.utilities

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior

class HideBottomViewOnScrollBehavior<V : View>(
    context: Context?,
    attrs: AttributeSet?
) : HideBottomViewOnScrollBehavior<V>(context, attrs) {

    public override fun slideDown(child: V) {
        super.slideDown(child)
    }

    public override fun slideUp(child: V) {
        super.slideUp(child)
    }
}
