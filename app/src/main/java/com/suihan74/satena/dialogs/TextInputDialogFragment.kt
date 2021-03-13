package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentDialogTextInputBinding
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.getIntOrNull
import com.suihan74.utilities.extensions.withArguments
import kotlinx.coroutines.launch

/**
 * 文字列入力用のダイアログ
 */
class TextInputDialogFragment : DialogFragment() {
    companion object {
        fun createInstance(
            @StringRes titleId: Int? = null,
            @StringRes descriptionId: Int? = null,
            @StringRes hintId: Int? = null,
            @StringRes neutralButtonTextId: Int? = null,
            initialValue: String? = ""
        ) = TextInputDialogFragment().withArguments {
            titleId?.let { putInt(Arg.TITLE_ID.name, it) }
            descriptionId?.let { putInt(Arg.DESCRIPTION_ID.name, it) }
            hintId?.let { putInt(Arg.HINT_ID.name, it) }
            neutralButtonTextId?.let { putInt(Arg.NEUTRAL_BUTTON_TEXT_ID.name, it) }
            putString(Arg.INITIAL_VALUE.name, initialValue)
        }

        enum class Arg {
            TITLE_ID,
            DESCRIPTION_ID,
            HINT_ID,
            NEUTRAL_BUTTON_TEXT_ID,
            INITIAL_VALUE
        }
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        DialogViewModel(requireArguments())
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_ok, null)

        viewModel.titleId?.let {
            builder.setTitle(it)
        }

        viewModel.descriptionId?.let {
            builder.setMessage(it)
        }

        viewModel.neutralButtonTextId?.let {
            builder.setNeutralButton(it, null)
        }

        val binding = FragmentDialogTextInputBinding.inflate(layoutInflater, null, false).also {
            it.vm = viewModel
            it.lifecycleOwner = this
        }
        builder.setView(binding.root)

        return builder.show().also { dialog ->
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                lifecycleScope.launch {
                    runCatching {
                        val value = viewModel.textValue.value.orEmpty()
                        if (viewModel.validator(value)) {
                            viewModel.onCompleteListener?.invoke(value)
                            dismiss()
                        }
                    }.onFailure {
                        Log.d("TextInputDialogFragment", Log.getStackTraceString(it))
                    }
                }
            }

            dialog.getButton(DialogInterface.BUTTON_NEUTRAL)?.setOnClickListener {
                lifecycleScope.launch {
                    runCatching {
                        val value = viewModel.textValue.value.orEmpty()
                        viewModel.onClickNeutralButton?.invoke(value, this@TextInputDialogFragment)
                    }.onFailure {
                        Log.d("TextInputDialogFragment", Log.getStackTraceString(it))
                    }
                }
            }
        }
    }

    // ------ //

    fun setValidator(v: SuspendSwitcher<String>?) = lifecycleScope.launchWhenCreated {
        viewModel.validator = v ?: { true }
    }

    fun setOnCompleteListener(l: SuspendListener<String>?) = lifecycleScope.launchWhenCreated {
        viewModel.onCompleteListener = l
    }

    fun setOnClickNeutralButtonListener(l: (suspend (value: String, f: TextInputDialogFragment)->Unit)?) = lifecycleScope.launchWhenCreated {
        viewModel.onClickNeutralButton = l
    }

    fun setText(value: String) = lifecycleScope.launchWhenCreated {
        viewModel.textValue.value = value
    }

    // ------ //

    class DialogViewModel(args: Bundle) : ViewModel() {
        @StringRes val titleId = args.getIntOrNull(Arg.TITLE_ID.name)
        @StringRes val descriptionId = args.getIntOrNull(Arg.DESCRIPTION_ID.name)
        @StringRes val hintId = args.getIntOrNull(Arg.HINT_ID.name)
        @StringRes val neutralButtonTextId = args.getIntOrNull(Arg.NEUTRAL_BUTTON_TEXT_ID.name)

        val textValue = MutableLiveData(args.getString(Arg.INITIAL_VALUE.name, ""))

        var validator : SuspendSwitcher<String> = { true }
        var onCompleteListener : SuspendListener<String>? = null
        var onClickNeutralButton : (suspend (value: String, f: TextInputDialogFragment)->Unit)? = null
    }
}
