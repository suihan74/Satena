package com.suihan74.satena.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.getIntOrNull
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel

/**
 * 数値設定用ダイアログ
 */
class NumberPickerDialog : DialogFragment() {
    companion object {
        /**
         * `NumberPickerDialog`のインスタンスを生成
         *
         * `default`が範囲外の場合は近い方の境界値に設定し直す
         *
         * @throws IllegalArgumentException `min > max`のとき送出
         */
        fun createInstance(
            min: Int,
            max: Int,
            default: Int,
            @StringRes titleId: Int? = null,
            @StringRes messageId: Int? = null,
            onComplete: Listener<Int>? = null
        ) = NumberPickerDialog().withArguments {
            if (min > max) throw IllegalArgumentException("min > max")

            putInt(ARG_MIN_VALUE, min)
            putInt(ARG_MAX_VALUE, max)
            putInt(ARG_DEFAULT_VALUE, when {
                default < min -> min
                default > max -> max
                else -> default
            })

            if (titleId != null) putInt(ARG_TITLE_ID, titleId)
            if (messageId != null) putInt(ARG_MESSAGE_ID, messageId)
        }.also {
            it.setOnCompleteListener(onComplete)
        }

        private const val ARG_MIN_VALUE = "ARG_MIN_VALUE"
        private const val ARG_MAX_VALUE = "ARG_MAX_VALUE"
        private const val ARG_DEFAULT_VALUE = "ARG_DEFAULT_VALUE"
        private const val ARG_TITLE_ID = "ARG_TITLE_ID"
        private const val ARG_MESSAGE_ID = "ARG_MESSAGE_ID"
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        DialogViewModel(requireArguments())
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val numberPicker = NumberPicker(themeWrappedContext()).also {
            it.minValue = viewModel.min
            it.maxValue = viewModel.max
            it.value = viewModel.current.value ?: viewModel.default

            it.setOnValueChangedListener { _, _, newVal ->
                viewModel.current.value = newVal
            }
        }

        return createBuilder().let { builder ->
            viewModel.titleId?.let { builder.setTitle(it) }
            viewModel.messageId?.let { builder.setMessage(requireContext().getString(it, viewModel.min, viewModel.max)) }
            builder.setView(numberPicker)
            builder.setNegativeButton(R.string.dialog_cancel, null)
            builder.setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.invokeOnComplete()
            }
            builder.create()
        }
    }

    // ------ //

    fun setOnCompleteListener(listener: Listener<Int>?) = lifecycleScope.launchWhenCreated {
        viewModel.onComplete = listener
    }

    // ------ //

    class DialogViewModel(args : Bundle) : ViewModel() {
        @StringRes val titleId = args.getIntOrNull(ARG_TITLE_ID)
        @StringRes val messageId = args.getIntOrNull(ARG_MESSAGE_ID)

        val min = args.getInt(ARG_MIN_VALUE)
        val max = args.getInt(ARG_MAX_VALUE)
        val default = args.getInt(ARG_DEFAULT_VALUE)

        val current = MutableLiveData(default)

        var onComplete : Listener<Int>? = null

        fun invokeOnComplete() {
            onComplete?.invoke(current.value!!)
        }
    }
}
