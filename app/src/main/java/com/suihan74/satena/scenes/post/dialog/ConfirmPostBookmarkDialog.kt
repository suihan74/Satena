package com.suihan74.satena.scenes.post.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleBookmarkBinding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.dialogs.setCustomTitle
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel

class ConfirmPostBookmarkDialog : DialogFragment() {
    companion object {
        fun createInstance(
            user: String,
            comment: String
        ) = ConfirmPostBookmarkDialog().withArguments {
            putString(ARG_USER, user)
            putString(ARG_COMMENT,
                if (comment.isBlank()) "(コメントなし)"
                else comment
            )
        }

        private const val ARG_USER = "ARG_USER"
        private const val ARG_COMMENT = "ARG_COMMENT"
    }

    private val viewModel by lazyProvideViewModel {
        val args = requireArguments()
        val user = args.getString(ARG_USER)!!
        val comment = args.getString(ARG_COMMENT)!!

        DialogViewModel(user, comment)
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogTitleBookmarkBinding.inflate(
            localLayoutInflater(),
            null,
            false
        ).apply {
            root.setCustomTitle(viewModel.user, viewModel.comment)
        }

        return createBuilder()
            .setCustomTitle(binding.root)
            .setMessage(R.string.confirm_post_bookmark)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.onApprovedToPost?.invoke(Unit)
            }
            .setNegativeButton(R.string.dialog_cancel) { _, _ ->
                viewModel.onCancel?.invoke(Unit)
            }
            .create()
    }

    // ------ //

    fun setOnApproveListener(listener: Listener<Unit>?) = lifecycleScope.launchWhenCreated {
        viewModel.onApprovedToPost = listener
    }

    fun setOnCancelListener(listener: Listener<Unit>?) = lifecycleScope.launchWhenCreated {
        viewModel.onCancel = listener
    }

    // ------ //

    class DialogViewModel(
        val user: String,
        val comment: String
    ) : ViewModel() {
        var onApprovedToPost : Listener<Unit>? = null
        var onCancel : Listener<Unit>? = null
    }
}
