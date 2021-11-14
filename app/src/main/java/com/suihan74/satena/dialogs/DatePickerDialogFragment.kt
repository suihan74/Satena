package com.suihan74.satena.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.utilities.DialogListener
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel
import java.time.LocalDate
import java.util.*

class DatePickerDialogFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {
    companion object {
        fun createInstance(
            initialValue: LocalDate? = null,
            min: LocalDate? = null,
            max: LocalDate? = null
        ) = DatePickerDialogFragment().withArguments {
            putObject(ARG_INITIAL_VALUE, initialValue)
            putObject(ARG_MIN, min)
            putObject(ARG_MAX, max)
        }

        private const val ARG_MIN = "ARG_MIN"
        private const val ARG_MAX = "ARG_MAX"
        private const val ARG_INITIAL_VALUE = "ARG_INITIAL_VALUE"
    }

    // ----- //

    private val viewModel by lazyProvideViewModel { DialogViewModel() }

    // ----- //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val date = args.getObject<LocalDate>(ARG_INITIAL_VALUE) ?: LocalDate.now()

        return DatePickerDialog(
            requireContext(),
            this,
            date.year, date.monthValue, date.dayOfMonth
        ).also { dialog ->
            args.getObject<LocalDate>(ARG_MIN)?.let { min ->
                dialog.datePicker.minDate =
                    GregorianCalendar(min.year, min.monthValue, min.dayOfMonth).timeInMillis
            }
            args.getObject<LocalDate>(ARG_MAX)?.let { max ->
                dialog.datePicker.maxDate =
                    GregorianCalendar(max.year, max.monthValue, max.dayOfMonth).timeInMillis
            }
        }
    }

    override fun onDateSet(picker: DatePicker?, year: Int, month: Int, day: Int) {
        viewModel.onCompletedListener?.invoke(
            LocalDate.of(year, month, day),
            this
        )
    }

    fun setOnCompletedListener(l : DialogListener<LocalDate>?) : DatePickerDialogFragment {
        lifecycleScope.launchWhenCreated {
            viewModel.onCompletedListener = l
        }
        return this
    }

    // ------ //

    class DialogViewModel : ViewModel() {
        var onCompletedListener : DialogListener<LocalDate>? = null
    }
}
