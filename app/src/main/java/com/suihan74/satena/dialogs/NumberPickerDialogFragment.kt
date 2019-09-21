package com.suihan74.satena.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.suihan74.satena.R

class NumberPickerDialogFragment : DialogFragment() {
    private var title : String? = null
    private var message : String? = null
    private var minValue : Int = 0
    private var maxValue : Int = 100
    private var defaultValue : Int = 0
    private var onCompleted : ((Int)->Unit)? = null

    companion object {
        fun createInstance(
            title: String,
            message: String,
            minValue: Int,
            maxValue: Int,
            defaultValue: Int = minValue,
            onCompleted: ((Int)->Unit)? = null
        ) = NumberPickerDialogFragment().apply {
            this.title = title
            this.message = message
            this.minValue = minValue
            this.maxValue = maxValue
            this.defaultValue = defaultValue
            this.onCompleted = onCompleted
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val numberPicker = object : NumberPicker(context) {
            override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
                super.addView(child, index, params)
                if (child is EditText) {
                    child.setTextColor(ActivityCompat.getColor(context, R.color.textColor))
                }
            }
        }.apply {
            minValue = this@NumberPickerDialogFragment.minValue
            maxValue = this@NumberPickerDialogFragment.maxValue
            value = defaultValue
        }

        return AlertDialog.Builder(activity, R.style.AlertDialogStyle)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                val value = numberPicker.value
                onCompleted?.invoke(value)
            }
            .setNegativeButton("CANCEL", null)
            .setView(numberPicker)
            .create()
    }
}
