package com.suihan74.utilities

import android.os.Handler
import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView

open class MutableLinkMovementMethod : LinkMovementMethod() {
    private val mHandler = Handler()
    private val mLongPressedRunnable = Runnable {
        if (!mHandled) {
            mHandled = true
            onLongPressed(mLink)
        }
    }
    private var mHandled = false
    private var mLink = ""

    open fun onSinglePressed(link: String) {
    }

    open fun onLongPressed(link: String) {
    }

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val lineSpacingExtra = widget.lineSpacingExtra
        val lineSpacingMultiplier = widget.lineSpacingMultiplier

        widget.movementMethod = this

        val action = event.action
        var result = false
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            val x = event.x - widget.totalPaddingLeft + widget.scrollX
            val y = event.y - widget.totalPaddingTop + widget.scrollY

            val layout = widget.layout
            val line = layout.getLineForVertical(y.toInt())
            val offset = layout.getOffsetForHorizontal(line, x)

            val link = buffer.getSpans(offset, offset, ClickableSpan::class.java)
            if (link.isNotEmpty()) {
                mLink = buffer.substring(buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]))
                when (action) {
                    MotionEvent.ACTION_UP -> {
                        mHandler.removeCallbacks(mLongPressedRunnable)
                        if (!mHandled) {
                            mHandled = true
                            onSinglePressed(mLink)
                        }
                    }

                    MotionEvent.ACTION_DOWN -> {
                        mHandled = false
                        mHandler.postDelayed(mLongPressedRunnable, ViewConfiguration.getLongPressTimeout().toLong())

                        Selection.setSelection(
                            buffer,
                            buffer.getSpanStart(link[0]),
                            buffer.getSpanEnd(link[0]))
                    }
                }
                result = true
            }
            else {
                Selection.removeSelection(buffer)
            }
        }
        else {
            if (action == MotionEvent.ACTION_CANCEL) {
                mHandled = true
                mHandler.removeCallbacks(mLongPressedRunnable)
            }
        }

        widget.movementMethod = null
        widget.isFocusable = false
        widget.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)

        return result
    }
}
