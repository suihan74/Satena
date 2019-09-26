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
import androidx.fragment.app.FragmentManager
import com.suihan74.satena.R

class NumberPickerDialogFragment : DialogFragment() {
    private class Members {
        var title : String? = null
        var message : String? = null
        var minValue : Int = 0
        var maxValue : Int = 100
        var defaultValue : Int = 0
        var onCompleted : ((FragmentManager, Int)->Unit)? = null
    }

    private var members = Members()

    companion object {
        fun createInstance(
            title: String,
            message: String,
            minValue: Int,
            maxValue: Int,
            defaultValue: Int = minValue,
            onCompleted: ((FragmentManager, Int)->Unit)? = null
        ) = NumberPickerDialogFragment().apply {
            members.run {
                this.title = title
                this.message = message
                this.minValue = minValue
                this.maxValue = maxValue
                this.defaultValue = defaultValue
                this.onCompleted = onCompleted
            }
        }

        private var savedMembers : Members? = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedMembers = members
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedMembers?.let {
            members = it
            savedMembers = null
        }

        val numberPicker = object : NumberPicker(context) {
            override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
                super.addView(child, index, params)
                if (child is EditText) {
                    child.setTextColor(ActivityCompat.getColor(context, R.color.textColor))
                }
            }
        }.apply {
            minValue = members.minValue
            maxValue = members.maxValue
            value = members.defaultValue
        }

        return AlertDialog.Builder(activity, R.style.AlertDialogStyle)
            .setTitle(members.title)
            .setMessage(members.message)
            .setPositiveButton("OK") { _, _ ->
                val value = numberPicker.value
                members.onCompleted?.invoke(fragmentManager!!, value)
            }
            .setNegativeButton("CANCEL", null)
            .setView(numberPicker)
            .create()
    }
}
