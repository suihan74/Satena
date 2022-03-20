package com.suihan74.utilities.extensions

import android.util.Log
import androidx.customview.widget.ViewDragHelper
import androidx.drawerlayout.widget.DrawerLayout

fun DrawerLayout.setTouchSlop(value: Int) {
    runCatching {
        val leftDraggerField = DrawerLayout::class.java.getDeclaredField("mLeftDragger").apply {
            isAccessible = true
        }
        val leftDragger = leftDraggerField.get(this) as ViewDragHelper

        val rightDraggerField = DrawerLayout::class.java.getDeclaredField("mRightDragger").apply {
            isAccessible = true
        }
        val rightDragger = rightDraggerField.get(this) as ViewDragHelper

        val touchSlopField = ViewDragHelper::class.java.getDeclaredField("mTouchSlop").apply {
            isAccessible = true
        }

        touchSlopField.setInt(leftDragger, value)
        touchSlopField.setInt(rightDragger, value)
    }.onFailure {
        Log.e("DrawerLayout", it.stackTraceToString())
    }
}

val DrawerLayout.leftTouchSlop : Int get() {
    val draggerField = DrawerLayout::class.java.getDeclaredField("mLeftDragger").apply {
        isAccessible = true
    }
    val leftDragger = draggerField.get(this)

    val touchSlopField = ViewDragHelper::class.java.getDeclaredField("mTouchSlop").apply {
        isAccessible = true
    }
    return touchSlopField.get(leftDragger) as Int
}

val DrawerLayout.rightTouchSlop : Int get() {
    val draggerField = DrawerLayout::class.java.getDeclaredField("mRightDragger").apply {
        isAccessible = true
    }
    val rightDragger = draggerField.get(this)

    val touchSlopField = ViewDragHelper::class.java.getDeclaredField("mTouchSlop").apply {
        isAccessible = true
    }
    return touchSlopField.get(rightDragger) as Int
}
