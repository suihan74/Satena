package com.suihan74.satena.scenes.post2.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.whenStarted
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.setCustomTitle
import com.suihan74.satena.scenes.post2.BookmarkPostViewModel
import com.suihan74.utilities.Listener
import com.suihan74.utilities.provideViewModel
import com.suihan74.utilities.withArguments

class ConfirmPostBookmarkDialog : DialogFragment() {
    companion object {
        fun createInstance(viewModel: BookmarkPostViewModel) = ConfirmPostBookmarkDialog().withArguments {
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

        private const val ARG_USER = "ARG_USER"
        private const val ARG_COMMENT = "ARG_COMMENT"
    }

    private val viewModel by lazy {
        provideViewModel(this) {
            val args  =requireArguments()
            val user = args.getString(ARG_USER)!!
            val comment = args.getString(ARG_COMMENT)!!

            DialogViewModel(user, comment)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val titleView = inflater.inflate(R.layout.dialog_title_bookmark, null).apply {
            setCustomTitle(viewModel.user, viewModel.comment)
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setCustomTitle(titleView)
            .setMessage(R.string.confirm_post_bookmark)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.onApprovedToPost?.invoke(Unit)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
    }

    suspend fun setOnApprovedToPost(listener: Listener<Unit>?) = whenStarted {
        viewModel.onApprovedToPost = listener
    }

    // ------ //

    class DialogViewModel(
        val user: String,
        val comment: String
    ) : ViewModel() {
        var onApprovedToPost : Listener<Unit>? = null
    }
}
