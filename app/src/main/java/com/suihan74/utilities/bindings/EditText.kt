package com.suihan74.utilities.bindings

import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.databinding.BindingAdapter

object EditTextBindingAdapters {
    /** 無効なリソースでエラー落ちしないようにした`android:hint` */
    @JvmStatic
    @BindingAdapter("android:hint")
    fun bindHintRes(editText: AppCompatEditText, @StringRes hintId: Int?) {
        editText.hint = hintId.let {
            if (it == null || it == 0x0) ""
            else editText.context.getText(it)
        }
    }
}
