package com.suihan74.satena.scenes.bookmarks2.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.launch

class UserTagSelectionDialog : DialogFragment() {
    class UserTagSelectionViewModel : ViewModel() {
        /** ダイアログでのタグ選択状態を保持する */
        lateinit var checks: BooleanArray
            private set

        fun init(initialChecks: BooleanArray) {
            checks = initialChecks.copyOf()
        }
    }

    companion object {
        fun createInstance(user: String) = UserTagSelectionDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_USER, user)
            }
        }
        private const val ARG_USER = "ARG_USER"
    }

    private lateinit var viewModel: UserTagSelectionViewModel

    private val DIALOG_NEW_TAG by lazy { "DIALOG_NEW_TAG" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[UserTagSelectionViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val listener = parentFragment as? Listener ?: activity as? Listener
        val user = requireArguments().getString(ARG_USER)!!
        val tags = listener?.getUserTags() ?: emptyList()

        val tagNames = tags.map { it.userTag.name }.toTypedArray()
        val initialChecks = tags.map { it.users.any { u -> u.name == user } }.toBooleanArray()

        if (savedInstanceState == null) {
            viewModel.init(initialChecks)
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.user_tags_dialog_title)
            .setMultiChoiceItems(
                tagNames,
                viewModel.checks
            ) { _, which, isChecked ->
                viewModel.checks[which] = isChecked
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .setNeutralButton(R.string.user_tags_dialog_new_tag) { _, _ ->
                val dialog = UserTagDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTargetUser(user)
                    .create()
                dialog.showAllowingStateLoss(parentFragmentManager, DIALOG_NEW_TAG)
            }
            .setPositiveButton(R.string.dialog_ok, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val inactiveTags = tags
                        .filterIndexed { idx, _ -> !viewModel.checks[idx] && initialChecks[idx] }
                        .map { it.userTag }

                    val activeTags = tags
                        .filterIndexed { idx, _ -> viewModel.checks[idx] && !initialChecks[idx] }
                        .map { it.userTag }

                    viewModel.viewModelScope.launch {
                        try {
                            listener?.inactivateTags(user, inactiveTags)
                            listener?.activateTags(user, activeTags)
                            listener?.reloadUserTags()
                        }
                        catch (e: Throwable) {
                            Log.e("userTagDialog", e.message ?: "")
                        }

                        dismiss()
                    }
                }
            }
    }

    interface Listener {
        /** ユーザータグのリストをダイアログに渡す */
        fun getUserTags() : List<TagAndUsers>
        /** 有効化するタグを渡す */
        suspend fun activateTags(user: String, activeTags: List<Tag>)
        /** 無効化するタグを渡す */
        suspend fun inactivateTags(user: String, inactiveTags: List<Tag>)
        /** activate/inactivate完了後にユーザータグ情報をリロードさせる */
        suspend fun reloadUserTags()
    }
}
