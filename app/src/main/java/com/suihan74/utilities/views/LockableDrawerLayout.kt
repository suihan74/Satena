package com.suihan74.utilities.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout

/**
 * スワイプクローズを禁止できるDrawerLayout
 *
 * 素のDrawerLayoutで
 * > drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
 * を使用すると、スワイプで開くこともできなくなるので、スワイプ処理を判定する処理を付加したやつを用意した
 */
class LockableDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleId: Int = 0
) : DrawerLayout(context, attrs, defStyleId) {
    private var closeSwipeEnabled : Boolean = true
    private var drawerView: View? = null

    fun setCloseSwipeEnabled(enabled: Boolean, drawerView: View? = null) {
        this.closeSwipeEnabled = enabled
        if (drawerView != null) {
            this.drawerView = drawerView
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        val view = drawerView ?: return super.onInterceptTouchEvent(ev)
        if (ev?.action == MotionEvent.ACTION_MOVE && !closeSwipeEnabled && isDrawerVisible(view)) {
            return false
        }
        return super.onInterceptTouchEvent(ev)
    }
}
