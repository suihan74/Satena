package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.whenStarted
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.fragment_dialog_user_tag.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserTagDialogFragment : DialogFragment() {
    companion object {
        fun createInstance(editingUserTag: Tag? = null) = UserTagDialogFragment().withArguments {
            putObject(ARG_EDITING_USER_TAG, editingUserTag)
        }

        private const val ARG_EDITING_USER_TAG = "ARG_EDITING_USER_TAG"
    }

    private val viewModel by lazy {
        provideViewModel(this) {
            DialogViewModel(
                requireArguments().getObject<Tag>(ARG_EDITING_USER_TAG)
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_user_tag, null)

        content.tag_name.run {
            setText(viewModel.tagName)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.tagName = text.toString()
                }
            })
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(viewModel.titleId)
            .setView(content)
            .setPositiveButton(R.string.dialog_register, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
            .apply {
                // IME表示を維持するための設定
                window?.run {
                    clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
                requireActivity().showSoftInputMethod(content.tag_name, WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

                // 登録
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    viewModel.invokeOnComplete(this@UserTagDialogFragment) { e -> when(e) {
                        is EmptyTagNameException ->
                            context.showToast(R.string.msg_user_tag_no_name)
                    } }
                }
            }
    }

    suspend fun setOnCompleteListener(listener: SuspendSwitcher<OnCompleteArguments>?) = whenStarted {
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

        fun invokeOnComplete(
            dialog: UserTagDialogFragment,
            onError: OnError?
        ) = viewModelScope.launch(Dispatchers.Main) {
            val tagName = tagName

            if (tagName.isBlank()) {
                onError?.invoke(EmptyTagNameException())
                return@launch
            }

            if (false != onComplete?.invoke(OnCompleteArguments(tagName, editingUserTag))) {
                dialog.dismiss()
            }
        }
    }

    data class OnCompleteArguments(
        val tagName: String,
        val editingUserTag: Tag?
    )
}
