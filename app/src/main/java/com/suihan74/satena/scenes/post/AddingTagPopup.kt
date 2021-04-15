package com.suihan74.satena.scenes.post

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.PopupWindow
import com.suihan74.satena.databinding.PopupAddTagBinding
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.dp2px

class AddingTagPopup(context: Context) : PopupWindow(context) {
    val binding = PopupAddTagBinding.inflate(
        LayoutInflater.from(context),
        null,
        false
    )

    init {
        contentView = binding.root
        width = context.dp2px(272)
        height = context.dp2px(54)
        elevation = context.dp2px(8).toFloat()
        isFocusable = true
        isOutsideTouchable = false

        binding.editText.setOnEditorActionListener { _, i, _ ->
            when (i) {
                EditorInfo.IME_ACTION_DONE -> {
                    complete()
                    true
                }
                else -> false
            }
        }

        binding.positiveButton.setOnClickListener {
            complete()
        }
    }

    // ------ //

    private var onCompleteListener : Listener<String>? = null

    fun setOnCompleteListener(l : Listener<String>) : AddingTagPopup {
        onCompleteListener = l
        return this
    }

    private fun complete() {
        onCompleteListener?.invoke(binding.editText.text?.toString().orEmpty())
        dismiss()
    }

    // ------ //

    override fun showAsDropDown(anchor: View?) {
        super.showAsDropDown(anchor)
        binding.editText.requestFocus()
    }
}
