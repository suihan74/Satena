package com.suihan74.utilities

import android.os.Handler
import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView

open class MutableLinkMovementMethod2 : LinkMovementMethod() {
    private val mHandler = Handler()
    private var mLongPressedRunnable: Runnable? = null
    private var mHandled = false

    open fun onSinglePressed(link: String) {
    }

    open fun onLongPressed(link: String) {
    }

    override fun onTouchEvent(
        widget: TextView, buffer: Spannable,
        event: MotionEvent
    ): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
            val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
            val line = widget.layout.getLineForVertical(y)
            val off = widget.layout.getOffsetForHorizontal(line, x.toFloat())

            val links = buffer.getSpans(
                off, off,
                ClickableSpan::class.java
            )

            if (links.isNotEmpty()) {
                val link = links[0]
                val linkText = buffer.substring(buffer.getSpanStart(link), buffer.getSpanEnd(link))

                when (action) {
                    MotionEvent.ACTION_UP -> {
                        if (mLongPressedRunnable != null) {
                            mHandler.removeCallbacks(mLongPressedRunnable!!)
                        }
                        if (!mHandled) {
                            mHandled = true
                            onSinglePressed(linkText)
                        }
                    }

                    MotionEvent.ACTION_DOWN -> {
                        mHandled = false
                        mLongPressedRunnable = Runnable {
                            if (!mHandled) {
                                mHandled = true
                                onLongPressed(linkText)
                            }
                        }
                        mHandler.postDelayed(
                            mLongPressedRunnable!!,
                            ViewConfiguration.getLongPressTimeout().toLong()
                        )

                        Selection.setSelection(
                            buffer,
                            buffer.getSpanStart(link),
                            buffer.getSpanEnd(link)
                        )
                    }
                }
                return true
            }
            else {
                Selection.removeSelection(buffer)
            }
        }

        return super.onTouchEvent(widget, buffer, event)
    }
}
