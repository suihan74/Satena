package com.suihan74.utilities

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.TextView
import android.widget.Toolbar

class MarqueeToolbar : Toolbar {
    constructor(context: Context, attributeSet: AttributeSet? = null) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) :
            super(context, attributeSet, defStyleAttr)

    /** タイトルの設定が完了しているか否か */
    private var reflected: Boolean = false
    /** タイトル部分のTextView */
    private var titleTextView: TextView? = null

    override fun setTitle(resId: Int) {
        if (!reflected) {
            reflected = reflectTitle()
        }
        super.setTitle(resId)
        selectTitle()
    }

    override fun setTitle(title: CharSequence?) {
        if (!reflected) {
            reflected = reflectTitle()
        }
        super.setTitle(title)
        selectTitle()
    }

    /** Viewが生成されたときに呼ばれる */
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && !reflected) {
            reflected = reflectTitle()
            selectTitle()
        }
    }

    /** タイトル部分のTextViewを設定 */
    private fun reflectTitle() =
        try {
            val field = Toolbar::class.java.getDeclaredField("mTitleTextView").apply {
                isAccessible = true
            }
            titleTextView = (field.get(this) as? TextView)?.apply {
                ellipsize = TextUtils.TruncateAt.MARQUEE
                marqueeRepeatLimit = -1   // forever
            }

            titleTextView != null
        }
        catch (e: Throwable) {
            e.printStackTrace()
            false
        }

    /** タイトル部分を選択 */
    private fun selectTitle() {
        titleTextView?.isSelected = true
    }
}
