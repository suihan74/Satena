package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentDialogUserTagBinding
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.utilities.SuspendSwitcher
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserTagDialogFragment : DialogFragment() {
    companion object {
        fun createInstance(
            editingUserTag: Tag? = null
        ) = UserTagDialogFragment().withArguments {
            putObject(ARG_EDITING_USER_TAG, editingUserTag)
        }

        private const val ARG_EDITING_USER_TAG = "ARG_EDITING_USER_TAG"
    }

    private val viewModel by lazyProvideViewModel {
        val args = requireArguments()
        val editingUserTag = args.getObject<Tag>(ARG_EDITING_USER_TAG)

        DialogViewModel(editingUserTag)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = localLayoutInflater()
        val binding = FragmentDialogUserTagBinding.inflate(inflater, null, false).also {
            it.vm = viewModel
            it.lifecycleOwner = parentFragment?.viewLifecycleOwner ?: activity
        }

        return createBuilder()
            .setTitle(viewModel.titleId)
            .setView(binding.root)
            .setPositiveButton(R.string.dialog_register, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
            .also { dialog ->
                // IME表示を維持するための設定
                dialog.showSoftInputMethod(requireActivity(), binding.tagName)

                // 登録
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    dialog.setButtonsEnabled(false)

                    lifecycleScope.launch(Dispatchers.Main) {
                        val result = runCatching {
                            viewModel.invokeOnComplete()
                        }

                        if (result.isSuccess) {
                            dialog.dismiss()
                        }
                        else {
                            when (result.exceptionOrNull()) {
                                is EmptyTagNameException ->
                                    showToast(R.string.msg_user_tag_no_name)
                            }

                            dialog.setButtonsEnabled(true)
                        }
                    }
                }
            }
    }

    fun setOnCompleteListener(
        listener: SuspendSwitcher<OnCompleteArguments>?
    ) = lifecycleScope.launchWhenCreated {
        viewModel.onComplete = listener
    }

    // ------ //

    class EmptyTagNameException : RuntimeException()

    // ------ //

    class DialogViewModel(
        /** 編集対象のタグ(新規作成時null) */
        private val editingUserTag: Tag?
    ) : ViewModel() {
        /** 既存タグの編集モードである */
        private val isModifyMode: Boolean by lazy {
            editingUserTag != null
        }

        /** ダイアログのタイトル */
        val titleId by lazy {
            if (isModifyMode) R.string.user_tag_dialog_title_edit_mode
            else R.string.user_tag_dialog_title_create_mode
        }

        /** 編集中のタグ名 */
        var tagName: String = editingUserTag?.name ?: ""

        var onComplete: SuspendSwitcher<OnCompleteArguments>? = null

        @Throws(
            EmptyTagNameException::class,
            TaskFailureException::class
        )
        suspend fun invokeOnComplete() {
            val tagName = tagName

            if (tagName.isBlank()) {
                throw EmptyTagNameException()
            }

            val result = runCatching {
                onComplete?.invoke(OnCompleteArguments(tagName, editingUserTag))
            }

            if (result.getOrDefault(false) != true) {
                throw TaskFailureException(cause = result.exceptionOrNull())
            }
        }
    }

    data class OnCompleteArguments(
        val tagName: String,
        val editingUserTag: Tag?
    )
}
