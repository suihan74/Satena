package com.suihan74.satena.scenes.post2.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.setCustomTitle
import com.suihan74.satena.scenes.post2.ViewModel

class ConfirmPostBookmarkDialog : DialogFragment() {
    companion object {
        fun createInstance(viewModel: ViewModel) = ConfirmPostBookmarkDialog().apply {
            arguments = Bundle().apply {
                putString(
                    ARG_USER,
                    viewModel.user
                )
                putString(
                    ARG_COMMENT,
                    if (viewModel.comment.value.isNullOrBlank()) "(コメントなし)"
                    else viewModel.comment.value!!
                )
            }
        }

        private const val ARG_USER = "ARG_USER"
        private const val ARG_COMMENT = "ARG_COMMENT"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val user = requireArguments().getString(ARG_USER)!!
        val comment = requireArguments().getString(ARG_COMMENT)!!

        val listener = parentFragment as? Listener ?: activity as? Listener

        val titleView = LayoutInflater.from(context).inflate(R.layout.dialog_title_bookmark, null).apply {
            setCustomTitle(user, comment)
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setCustomTitle(titleView)
            .setMessage(R.string.confirm_post_bookmark)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                listener?.onApprovedToPost(this)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
    }

    interface Listener {
        /** 投稿が承認された場合 */
        fun onApprovedToPost(dialog: ConfirmPostBookmarkDialog)
    }
}
