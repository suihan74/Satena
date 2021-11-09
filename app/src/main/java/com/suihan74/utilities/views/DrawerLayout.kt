package com.suihan74.utilities.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import com.suihan74.utilities.extensions.dp2px

@SuppressLint("RtlHardcoded")
open class DrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleId: Int = 0
) : DrawerLayout(context, attrs, defStyleId) {

    private var exclusionRects : List<Rect>? = null
    private var edgeGravity : Int = Gravity.RIGHT

    init {
        addDrawerListener(object : DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    systemGestureExclusionRects = emptyList()
                }
            }
            override fun onDrawerClosed(drawerView: View) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    systemGestureExclusionRects = exclusionRects.orEmpty()
                }
            }
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        updateSystemGestureExclusionRects(r, b)
    }

    private fun updateSystemGestureExclusionRects(r: Int, b: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }
        val dp200 = context.dp2px(200)
        val dp32 = context.dp2px(32)
        val gestureExclusionRect =
            if (edgeGravity == Gravity.LEFT) Rect(0, b - dp200, dp32, b)
            else Rect(r - dp32, b - dp200, r, b)

        exclusionRects = listOf(gestureExclusionRect)
        systemGestureExclusionRects = exclusionRects!!
    }

    fun setGravity(gravity: Int) {
        val layoutDirection = resources.configuration.layoutDirection
        edgeGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection)
    }
}
