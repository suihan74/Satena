package com.suihan74.utilities.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.R
import androidx.appcompat.widget.AppCompatEditText
import com.suihan74.utilities.extensions.alsoAs

class ImeEnhancedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleId: Int = R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleId) {

    /** IMEが戻るボタンで閉じられたときにフォーカスを失うようにする */
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
            parent?.alsoAs<View> { parent ->
                parent.isFocusable = true
                parent.isFocusableInTouchMode = true
                parent.requestFocus()
            }
        }
        return super.onKeyPreIme(keyCode, event)
    }
}
