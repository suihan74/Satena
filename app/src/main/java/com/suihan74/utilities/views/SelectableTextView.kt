package com.suihan74.utilities.views

import android.content.Context
import android.text.Selection
import android.text.Spannable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import com.suihan74.utilities.extensions.alsoAs

/**
 * [LinkMovementMethodを設定したTextViewの文字列選択を解除するとクラッシュする](https://suihan74.github.io/posts/2020/01_06_01_selectable_textview/)
 *
 * 問題を回避したAppCompatTextView
 */
class SelectableTextView : AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet? = null) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        // setText(...)を呼ぶことで回避される
        if (selectionStart != selectionEnd && event?.actionMasked == MotionEvent.ACTION_DOWN) {
            text = text
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (selStart == -1 || selEnd == -1) {
            text.alsoAs<Spannable> {
                Selection.setSelection(it, 0, 0)
            }
        }
        else {
            super.onSelectionChanged(selStart, selEnd)
        }
    }
}
