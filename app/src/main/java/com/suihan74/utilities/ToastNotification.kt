package com.suihan74.utilities

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import com.suihan74.satena.R


fun Context.showToast(message: String) {
    val dimen = this.resources.getDimension(R.dimen.toast_offset_y)
    val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT).apply {
        setGravity(Gravity.TOP, 0, dimen.toInt())
    }
    toast.show()
}
