package com.suihan74.satena.scenes.bookmarks.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentDialogConfirmBrowsingBinding
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.utilities.DialogListener
import com.suihan74.utilities.lazyProvideViewModel

/**
 * ツールバータップでエントリをアプリ内ブラウザで開く確認ダイアログ
 */
class ConfirmBrowsingDialog : DialogFragment() {
    companion object {
        fun createInstance(entry: Entry) = ConfirmBrowsingDialog().also {
            it.setEntry(entry)
        }
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        DialogViewModel()
    }

    private var _binding : FragmentDialogConfirmBrowsingBinding? = null
    private val binding get() = _binding!!

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentDialogConfirmBrowsingBinding.inflate(localLayoutInflater(), null, false).also {
            it.vm = viewModel
            it.lifecycleOwner = this
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_dialog_title_simple)
            .setView(binding.root)
            .setPositiveButton(R.string.bookmark_confirm_browsing_dialog_positive_action) { _, _ ->
                viewModel.onPositiveListener?.invoke(viewModel.notShowAgain.value == true, this)
            }
            .setNegativeButton(R.string.bookmark_confirm_browsing_dialog_negative_action) { _, _ ->
                viewModel.onNegativeListener?.invoke(viewModel.notShowAgain.value == true, this)
            }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ------ //

    fun setEntry(entry: Entry) = lifecycleScope.launchWhenCreated {
        viewModel.entry.value = entry
    }

    fun setPositiveButtonListener(l : DialogListener<Boolean>?) = lifecycleScope.launchWhenCreated {
        viewModel.onPositiveListener = l
    }

    fun setNegativeButtonListener(l : DialogListener<Boolean>?) = lifecycleScope.launchWhenCreated {
        viewModel.onNegativeListener = l
    }

    // ------ //

    class DialogViewModel : ViewModel() {
        /** 対象エントリ */
        val entry = MutableLiveData<Entry>()

        /** 次回から表示しない */
        val notShowAgain = MutableLiveData<Boolean>()

        var onPositiveListener : DialogListener<Boolean>? = null

        var onNegativeListener : DialogListener<Boolean>? = null
    }
}
