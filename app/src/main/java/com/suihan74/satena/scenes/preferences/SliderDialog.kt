package com.suihan74.satena.scenes.preferences

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentDialogSliderBinding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.utilities.DialogListener
import com.suihan74.utilities.extensions.getIntOrNull
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel

class SliderDialog : DialogFragment() {

    companion object {
        fun <NumberT : Number> createInstance(
            @StringRes titleId: Int?,
            @StringRes messageId: Int?,
            min: NumberT,
            max: NumberT,
            value: NumberT,
        ) = SliderDialog().withArguments { f ->
            titleId?.let { putInt(Args.TITLE_ID.name, it) }
            messageId?.let { putInt(Args.MESSAGE_ID.name, it) }
            f.lifecycleScope.launchWhenCreated {
                f.viewModel.min.value = min.toFloat()
                f.viewModel.max.value = max.toFloat()
                f.viewModel.current.value = value.toFloat()
            }
        }

        enum class Args {
            TITLE_ID,
            MESSAGE_ID
        }
    }

    // ------ //

    private val viewModel by lazyProvideViewModel { DialogViewModel() }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = createBuilder()
        val binding = FragmentDialogSliderBinding.inflate(localLayoutInflater(), null, false).also {
            it.vm = viewModel
            it.lifecycleOwner = this
        }

        arguments?.let { args ->
            args.getIntOrNull(Args.TITLE_ID.name)?.let {
                builder.setTitle(it)
            }
            args.getIntOrNull(Args.MESSAGE_ID.name)?.let {
                builder.setMessage(it)
            }
        }

        return builder
            .setView(binding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.invokeOnComplete(this)
            }
            .create()
    }

    // ------ //

    fun setOnCompleteListener(l : DialogListener<Float>?) : SliderDialog {
        lifecycleScope.launchWhenCreated {
            viewModel.onComplete = l
        }
        return this
    }

    // ------ //

    class DialogViewModel : ViewModel() {
        val min = MutableLiveData<Float>()
        val max = MutableLiveData<Float>()
        val current = MutableLiveData<Float>()

        var onComplete : DialogListener<Float>? = null

        fun invokeOnComplete(fragment: DialogFragment) {
            onComplete?.invoke(current.value!!, fragment)
        }
    }
}
