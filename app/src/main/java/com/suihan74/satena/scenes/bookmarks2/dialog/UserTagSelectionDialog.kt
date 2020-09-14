package com.suihan74.satena.scenes.bookmarks2.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.whenStarted
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.utilities.Listener
import com.suihan74.utilities.SuspendListener
import com.suihan74.utilities.provideViewModel
import com.suihan74.utilities.withArguments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserTagSelectionDialog : DialogFragment() {
    companion object {
        fun createInstance(user: String) = UserTagSelectionDialog().withArguments {
            putString(ARG_USER, user)
        }

        private const val ARG_USER = "ARG_USER"
    }

    private val viewModel: DialogViewModel by lazy {
        provideViewModel(this) {
            val args = requireArguments()
            // TODO: タグ取得方法の変更
            val tags = (requireActivity() as BookmarksActivity).viewModel.getUserTags()

            DialogViewModel(
                args.getString(ARG_USER)!!,
                tags
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.user_tags_dialog_title)
            .setMultiChoiceItems(viewModel.tagNames, viewModel.checks) { _, which, isChecked ->
                viewModel.checks[which] = isChecked
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .setNeutralButton(R.string.user_tags_dialog_new_tag) { _, _ ->
                viewModel.onAddNewTag?.invoke(Unit)
            }
            .setPositiveButton(R.string.dialog_ok, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    viewModel.invokeOnComplete(this@UserTagSelectionDialog)
                }
            }
    }

    suspend fun setOnAddNewTagListener(listener: Listener<Unit>?) = whenStarted {
        viewModel.onAddNewTag = listener
    }

    suspend fun setOnActivateTagsListener(listener: SuspendListener<ActivateUserTagsArguments>?) = whenStarted {
        viewModel.onActivateTags = listener
    }

    suspend fun setOnInactivateTagsListener(listener: SuspendListener<ActivateUserTagsArguments>?) = whenStarted {
        viewModel.onInactivateTags = listener
    }

    suspend fun setOnCompleteListener(listener: SuspendListener<Unit>?) = whenStarted {
        viewModel.onComplete = listener
    }

    // ------ //

    class DialogViewModel(
        val user: String,
        val tags: List<TagAndUsers>
    ) : ViewModel() {
        val tagNames: Array<String> by lazy {
            tags.map { it.userTag.name }.toTypedArray()
        }

        /** ダイアログでのタグ選択状態を保持する */
        val checks: BooleanArray by lazy {
            initialChecks.clone()
        }

        /** ダイアログが開かれた時点での選択状態 */
        val initialChecks: BooleanArray =
            tags.map { it.users.any { u -> u.name == user } }.toBooleanArray()

        /** 選択状態から非選択状態に変更されたアイテム */
        val inactivatedTags: List<Tag>
            get() = tags
                .filterIndexed { idx, _ -> !checks[idx] && initialChecks[idx] }
                .map { it.userTag }

        /** 非選択状態から選択状態に変更されたアイテム */
        val activatedTags: List<Tag>
            get() = tags
                .filterIndexed { idx, _ -> checks[idx] && !initialChecks[idx] }
                .map { it.userTag }

        // --- //

        /** 新規タグ作成 */
        var onAddNewTag: Listener<Unit>? = null

        /** タグを有効化する */
        var onActivateTags: SuspendListener<ActivateUserTagsArguments>? = null

        /** タグを無効化する */
        var onInactivateTags: SuspendListener<ActivateUserTagsArguments>? = null

        /** 有効化・無効化が完了 */
        var onComplete: SuspendListener<Unit>? = null

        fun invokeOnComplete(dialog: UserTagSelectionDialog) = viewModelScope.launch(Dispatchers.Main) {
            try {
                onActivateTags?.invoke(ActivateUserTagsArguments(user, activatedTags))
                onInactivateTags?.invoke(ActivateUserTagsArguments(user, inactivatedTags))
                onComplete?.invoke(Unit)
            }
            catch (e: Throwable) {
                Log.e("userTagSelection", Log.getStackTraceString(e))
            }
            finally {
                dialog.dismiss()
            }
        }
    }

    data class ActivateUserTagsArguments(
        val user: String,
        val tags: List<Tag>
    )
}
