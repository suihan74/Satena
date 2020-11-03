package com.suihan74.satena.scenes.bookmarks.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.utilities.Listener
import com.suihan74.utilities.SuspendListener
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.setButtonsEnabled
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserTagSelectionDialog : DialogFragment() {
    companion object {
        fun createInstance(
            user: String,
            tags: List<Tag>,
            initialCheckedTagIds: List<Int>
        ) = UserTagSelectionDialog().withArguments {
            putString(ARG_USER, user)
            putIntArray(ARG_INITIAL_CHECKED_IDS, initialCheckedTagIds.toIntArray())
            it.setUserTags(tags)
        }

        private const val ARG_USER = "ARG_USER"
        private const val ARG_INITIAL_CHECKED_IDS = "ARG_INITIAL_CHECKED_IDS"
    }

    private val viewModel: DialogViewModel by lazy {
        provideViewModel(this) {
            val args = requireArguments()
            val user = args.getString(ARG_USER)!!
            val initialCheckedTagIds = args.getIntArray(ARG_INITIAL_CHECKED_IDS) ?: IntArray(0)
            DialogViewModel(user, initialCheckedTagIds)
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
            .also { dialog ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    // 処理中の操作を禁止する
                    dialog.setButtonsEnabled(false)
                    lifecycleScope.launch(Dispatchers.Main) {
                        val result = runCatching {
                            viewModel.invokeOnComplete()
                        }

                        if (result.isSuccess) {
                            dialog.dismiss()
                        }
                        else {
                            Log.e("UserTagSelection", Log.getStackTraceString(result.exceptionOrNull()))
                            context?.showToast(R.string.msg_user_tag_selection_failure)
                            dialog.setButtonsEnabled(true)
                        }
                    }
                }
            }
    }

    fun setUserTags(
        tags: List<Tag>
    ) = lifecycleScope.launchWhenCreated {
        viewModel.tags = tags
    }

    fun setOnAddNewTagListener(
        l: Listener<Unit>?
    ) = lifecycleScope.launchWhenCreated {
        viewModel.onAddNewTag = l
    }

    fun setOnActivateTagsListener(
        l: SuspendListener<ActivateUserTagsArguments>?
    ) = lifecycleScope.launchWhenCreated {
        viewModel.onActivateTags = l
    }

    fun setOnInactivateTagsListener(
        l: SuspendListener<ActivateUserTagsArguments>?
    ) = lifecycleScope.launchWhenCreated {
        viewModel.onInactivateTags = l
    }

    fun setOnCompleteListener(
        l: SuspendListener<Unit>?
    ) = lifecycleScope.launchWhenCreated {
        viewModel.onComplete = l
    }

    // ------ //

    class DialogViewModel(
        val user: String,
        private val initialCheckedTagIds: IntArray
    ) : ViewModel() {
        var tags: List<Tag> = emptyList()

        val tagNames: Array<String> by lazy {
            tags.map { it.name }.toTypedArray()
        }

        /** ダイアログでのタグ選択状態を保持する */
        val checks: BooleanArray by lazy {
            initialChecks.clone()
        }

        /** ダイアログが開かれた時点での選択状態 */
        val initialChecks: BooleanArray by lazy {
            tags.map { initialCheckedTagIds.contains(it.id) }.toBooleanArray()
        }

        /** 選択状態から非選択状態に変更されたアイテム */
        val inactivatedTags: List<Tag>
            get() = tags
                .filterIndexed { idx, _ -> !checks[idx] && initialChecks[idx] }

        /** 非選択状態から選択状態に変更されたアイテム */
        val activatedTags: List<Tag>
            get() = tags
                .filterIndexed { idx, _ -> checks[idx] && !initialChecks[idx] }

        // --- //

        /** 新規タグ作成 */
        var onAddNewTag: Listener<Unit>? = null

        /** タグを有効化する */
        var onActivateTags: SuspendListener<ActivateUserTagsArguments>? = null

        /** タグを無効化する */
        var onInactivateTags: SuspendListener<ActivateUserTagsArguments>? = null

        /** 有効化・無効化が完了 */
        var onComplete: SuspendListener<Unit>? = null

        @Throws(TaskFailureException::class)
        suspend fun invokeOnComplete() {
            val result = runCatching {
                onActivateTags?.invoke(
                    ActivateUserTagsArguments(user, activatedTags)
                )
                onInactivateTags?.invoke(
                    ActivateUserTagsArguments(user, inactivatedTags)
                )
            }

            if (result.isSuccess) {
                runCatching {
                    onComplete?.invoke(Unit)
                }
            }
            if (result.isFailure) {
                throw TaskFailureException(cause = result.exceptionOrNull())
            }
        }
    }

    data class ActivateUserTagsArguments(
        val user: String,
        val tags: List<Tag>
    )
}
