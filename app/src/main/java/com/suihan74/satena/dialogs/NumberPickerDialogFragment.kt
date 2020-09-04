package com.suihan74.satena.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager
import com.suihan74.satena.R
import com.suihan74.utilities.OnError
import com.suihan74.utilities.showAllowingStateLoss

class NumberPickerDialogFragment : AlertDialogFragment() {
    interface Listener {
        fun onCompleteNumberPicker(value: Int, dialog: NumberPickerDialogFragment)
    }

    companion object {
        private const val KEY_BASE = "NumberPickerDialog."
        const val MIN_VALUE = KEY_BASE + "MIN_VALUE"
        const val MAX_VALUE = KEY_BASE + "MAX_VALUE"
        const val DEFAULT_VALUE = KEY_BASE + "DEFAULT_VALUE"

        private const val CURRENT_VALUE = KEY_BASE + "CURRENT_VALUE"
    }

    var currentValue: Int = 0
        private set

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CURRENT_VALUE, currentValue)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val listener = parentFragment as? Listener ?: activity as? Listener
        val arguments = requireArguments()
        val minValue = arguments.getInt(MIN_VALUE)
        val maxValue = arguments.getInt(MAX_VALUE)
        val defaultValue = arguments.getInt(DEFAULT_VALUE)

        currentValue = savedInstanceState?.getInt(CURRENT_VALUE) ?: defaultValue

        val numberPicker =
            object : NumberPicker(context) {
                override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
                    super.addView(child, index, params)
                    if (child is EditText) {
                        child.setTextColor(ActivityCompat.getColor(context, R.color.textColor))
                    }
                }
            }.apply {
                this.minValue = minValue
                this.maxValue = maxValue
                value = currentValue

                setOnValueChangedListener { _, _, newVal -> currentValue = newVal }
            }

        return createBuilder(arguments, savedInstanceState)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                listener?.onCompleteNumberPicker(currentValue, this)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .setView(numberPicker)
            .create()
    }


    class NumberPickerException : RuntimeException {
        constructor() : super()
        constructor(msg: String, cause: Throwable? = null) : super(msg, cause)
        constructor(cause: Throwable) : super(cause)
    }

    class Builder(themeResId: Int) {
        private val arguments = Bundle().apply {
            putInt(THEME_RES_ID, themeResId)
            putInt(MIN_VALUE, 0)
            putInt(MAX_VALUE, 100)
            putInt(DEFAULT_VALUE, 0)
        }

        fun create() : NumberPickerDialogFragment{
            val minValue = arguments.getInt(MIN_VALUE)
            val maxValue = arguments.getInt(MAX_VALUE)

            when {
                minValue > maxValue ->
                    throw NumberPickerException("the min value is greater than the max value")

                minValue == maxValue ->
                    throw NumberPickerException("the min value equals to the max value")
            }

            // 初期値を範囲内に修正する
            val defaultValue = arguments.getInt(DEFAULT_VALUE)
            when {
                defaultValue < minValue ->
                    arguments.putInt(DEFAULT_VALUE, minValue)

                defaultValue > maxValue ->
                    arguments.putInt(DEFAULT_VALUE, maxValue)
            }

            return NumberPickerDialogFragment().apply {
                arguments = this@Builder.arguments
            }
        }

        fun show(fragmentManager: FragmentManager, tag: String) =
            create().show(fragmentManager, tag)

        fun showAllowingStateLoss(
            fragmentManager: FragmentManager,
            tag: String?,
            onError: OnError? = { Log.e("NumberPickerDialog", Log.getStackTraceString(it)) }) =
            create().showAllowingStateLoss(fragmentManager, tag, onError)

        fun setTitle(titleId: Int) = this.apply {
            arguments.putInt(TITLE_ID, titleId)
        }

        fun setMessage(messageId: Int) = this.apply {
            arguments.putInt(MESSAGE_ID, messageId)
        }

        fun setMaxValue(value: Int) = this.apply {
            arguments.putInt(MAX_VALUE, value)
        }

        fun setMinValue(value: Int) = this.apply {
            arguments.putInt(MIN_VALUE, value)
        }

        fun setDefaultValue(value: Int) = this.apply {
            arguments.putInt(DEFAULT_VALUE, value)
        }
    }
}
