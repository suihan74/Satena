package com.suihan74.satena.scenes.post.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.satena.databinding.FragmentDialogAddingTagBinding
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.showSoftInputMethod

class AddingTagDialog : BottomSheetDialogFragment() {

    companion object {
        fun createInstance() = AddingTagDialog()
    }

    // ------ //

    private var _binding : FragmentDialogAddingTagBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState != null) {
            dismiss()
            return null
        }

        _binding = FragmentDialogAddingTagBinding.inflate(inflater, container, false)

        binding.editText.setOnEditorActionListener { _, i, _ ->
            when (i) {
                EditorInfo.IME_ACTION_DONE -> {
                    complete()
                    true
                }
                else -> false
            }
        }
        dialog?.showSoftInputMethod(requireActivity(), binding.editText)

        binding.addButton.setOnClickListener {
            addTag()
        }

        binding.positiveButton.setOnClickListener {
            complete()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val listener = parentFragment as? OnDismissListener ?: requireActivity() as? OnDismissListener
        listener?.onDismiss(this)
    }

    // ------ //

    private var onCompleteListener : Listener<String>? = null

    fun setOnCompleteListener(l : Listener<String>) : AddingTagDialog {
        onCompleteListener = l
        return this
    }

    private fun complete() {
        addTag()
        dismiss()
    }

    private fun addTag() {
        binding.editText.text?.toString().orEmpty().let { tag ->
            runCatching {
                onCompleteListener?.invoke(tag)
            }
            binding.editText.text?.clear()
        }
    }

    // ------ //

    interface OnDismissListener {
        fun onDismiss(dialog: AddingTagDialog)
    }
}
