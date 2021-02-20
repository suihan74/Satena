package com.suihan74.satena.scenes.preferences.browser

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentDialogStartPageUrlEditingBinding
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserRepository
import com.suihan74.satena.scenes.preferences.pages.BrowserFragment
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.letAs
import com.suihan74.utilities.extensions.showSoftInputMethod
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StartPageUrlEditingDialog : DialogFragment() {
    companion object {
        fun createInstance() = StartPageUrlEditingDialog()
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        val repository = parentFragment.letAs<BrowserFragment, BrowserRepository> { it.viewModel.browserRepo }
            ?: BrowserRepository(
                HatenaClient,
                SafeSharedPreferences.create(requireContext()),
                SafeSharedPreferences.create(requireContext())
            )
        DialogViewModel(repository)
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentDialogStartPageUrlEditingBinding.inflate(
            layoutInflater, null, false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = parentFragment?.viewLifecycleOwner ?: activity
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle(R.string.pref_browser_start_page_desc)
            .setView(binding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_register) { _, _ ->
                viewModel.viewModelScope.launch {
                    viewModel.save()
                    lifecycleScope.launchWhenResumed {
                        dismiss()
                    }
                }
            }

        if (activity is BrowserActivity) {
            builder.setNeutralButton(R.string.pref_browser_start_page_set_current, null)
        }

        return builder.show()
            .also { dialog ->
                dialog.showSoftInputMethod(requireActivity(), binding.editText)

                // 表示中ページのURLをコピーする
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL)?.setOnClickListener {
                    activity.alsoAs<BrowserActivity> { activity ->
                        viewModel.url.value = activity.viewModel.url.value.orEmpty()
                    }
                }
            }
    }

    // ------ //

    class DialogViewModel(private val repository: BrowserRepository) : ViewModel() {
        val url = MutableLiveData<String>(repository.startPage.value.orEmpty())

        // ----- //

        suspend fun save() = withContext(Dispatchers.Main) {
            repository.startPage.value = url.value
        }
    }
}
