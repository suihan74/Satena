package com.suihan74.utilities

import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.widget.TextView

open class MutableLinkMovementMethod(
    private val onItemClicked: ((String)->Unit)? = null
) : LinkMovementMethod() {
    override fun onTouchEvent(widget: TextView?, buffer: Spannable?, event: MotionEvent?): Boolean {
        val action = event!!.action

        if (action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_DOWN
        ) {
            widget!!
            buffer!!
            val x = event.x - widget.totalPaddingLeft + widget.scrollX
            val y = event.y - widget.totalPaddingTop + widget.scrollY

            val layout = widget.layout
            val line = layout.getLineForVertical(y.toInt())
            val offset = layout.getOffsetForHorizontal(line, x)

            val link = buffer.getSpans(offset, offset, ClickableSpan::class.java)
            if (link.isNotEmpty()) {
                when (action) {
                    MotionEvent.ACTION_UP -> {
                        if (link[0] is ClickableSpan && onItemClicked != null) {
                            onItemClicked.invoke(buffer.substring(buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0])))
                        }
                        else {
                            link[0].onClick(widget)
                        }
                    }

                    MotionEvent.ACTION_DOWN -> {
                        Selection.setSelection(
                            buffer,
                            buffer.getSpanStart(link[0]),
                            buffer.getSpanEnd(link[0]))
                    }
                }
                return true
            }
            else {
                Selection.removeSelection(buffer)
            }
        }

        return false
    }
}
