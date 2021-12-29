package com.suihan74.satena.scenes.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.satena.databinding.FragmentDialogBrowserInformationBinding
import com.suihan74.satena.scenes.entries2.dialog.ShareEntryDialog
import com.suihan74.utilities.extensions.requireActivity

/**
 * 表示中ページ情報ダイアログ
 */
class InformationDialog : BottomSheetDialogFragment() {
    companion object {
        fun createInstance() = InformationDialog()
    }

    // ------ //

    val browserActivity
        get() = requireActivity<BrowserActivity>()

    val browserViewModel : BrowserViewModel
        get() = browserActivity.viewModel

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) : View {
        val binding = FragmentDialogBrowserInformationBinding.inflate(
            inflater,
            container,
            false
        ).also {
            it.vm = browserViewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        binding.okButton.setOnClickListener {
            dismiss()
        }

        binding.shareButton.setOnClickListener {
            ShareEntryDialog
                .createInstance(browserViewModel.url.value!!, browserViewModel.title.value!!)
                .show(childFragmentManager, null)
        }

        return binding.root
    }
}
