package com.suihan74.utilities.views

import android.app.Service
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.R
import androidx.appcompat.widget.AppCompatEditText
import com.suihan74.utilities.extensions.alsoAs

class ImeEnhancedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleId: Int = R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleId) {

    fun hideSoftInputMethod(nextFocus: View? = null) {
        val imm = context.getSystemService(Service.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
        clearFocus()
        (nextFocus ?: parent as? View)?.let {
            it.isFocusable = true
            it.isFocusableInTouchMode = true
            it.requestFocus()
        }
    }

    override fun clearFocus() {
        isFocusable = false
        isFocusableInTouchMode = false
        super.clearFocus()
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) {
            hideSoftInputMethod()
        }
    }

    /** IMEが戻るボタンで閉じられたときにフォーカスを失うようにする */
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
            clearFocus()
            parent?.alsoAs<View> { parent ->
                parent.isFocusable = true
                parent.isFocusableInTouchMode = true
                parent.requestFocus()
            }
        }
        return super.onKeyPreIme(keyCode, event)
    }
}
