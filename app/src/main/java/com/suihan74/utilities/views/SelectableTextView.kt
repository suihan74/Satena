package com.suihan74.utilities.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView

/**
 * [LinkMovementMethodを設定したTextViewの文字列選択を解除するとクラッシュする](https://suihan74.github.io/posts/2020/01_06_01_selectable_textview/)
 *
 * 問題を回避したAppCompatTextView
 */
class SelectableTextView : AppCompatTextView {
    constructor(context: Context, attributeSet: AttributeSet? = null) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) :
            super(context, attributeSet, defStyleAttr)

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        // setText(...)を呼ぶことで回避される
        if (selectionStart != selectionEnd && event?.actionMasked == MotionEvent.ACTION_DOWN) {
            text = text
        }
        return super.dispatchTouchEvent(event)
    }
}
