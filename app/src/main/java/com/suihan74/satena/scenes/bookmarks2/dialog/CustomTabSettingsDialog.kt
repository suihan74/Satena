package com.suihan74.satena.scenes.bookmarks2.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.scenes.bookmarks2.tab.CustomTabViewModel

private typealias Item = Triple<String, Boolean, (Int, Boolean)->Unit>

class CustomTabSettingsDialog : DialogFragment() {
    companion object {
        fun createInstance() = CustomTabSettingsDialog()
    }

    /** 設定値を保持するためのViewModel */
    private lateinit var viewModel: ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this)[ViewModel::class.java]

        if (savedInstanceState == null) {
            val listener =
                parentFragment as? Listener
                ?: activity as? Listener
                ?: throw IllegalStateException("")

            viewModel.init(listener.getCustomTabSettings(), listener.getUserTags())
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val listener = parentFragment as? Listener ?: activity as? Listener
        val set = viewModel.settings

        // タグ以外の設定項目部分
        val staticItems = listOf<Item>(
            Triple(
                getString(R.string.custom_bookmarks_no_comment_active),
                set.activeNoCommentBookmarks,
                { _, b -> viewModel.apply(activeNoCommentBookmarks = b) }
            ),

            Triple(
                getString(R.string.custom_bookmarks_ignored_user_active),
                set.activeMutedBookmarks,
                { _, b -> viewModel.apply(activeMutedBookmarks = b) }
            ),

            Triple(
                getString(R.string.custom_bookmarks_no_user_tags_active),
                set.activeUnaffiliatedUsers,
                { _, b -> viewModel.apply(activeUnaffiliatedUsers = b) }
            )
        )

        // 全てのタグリスト
        val tagItems = viewModel.tags.mapIndexed { idx, it ->
            Item (
                it.userTag.name,
                viewModel.states[idx]
            ) { which, b -> viewModel.setTagState(which, b) }
        }

        val items = staticItems.plus(tagItems)

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.custom_bookmarks_tab_pref_dialog_title)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                listener?.onPositiveButtonClicked(viewModel.createResult())
            }
            .setMultiChoiceItems(
                items.map { it.first }.toTypedArray(),
                items.map { it.second }.toBooleanArray()
            ) { _, which, checked ->
                if (which < staticItems.size) {
                    items[which].third(which, checked)
                }
                else {
                    items[which].third(which - staticItems.size, checked)
                }
            }
            .create()
    }

// --- Listener --- //

    interface Listener {
        fun getCustomTabSettings() : CustomTabViewModel.Settings
        fun getUserTags() : List<TagAndUsers>
        fun onPositiveButtonClicked(set: CustomTabViewModel.Settings)
    }

// --- ViewModel --- //

    class ViewModel: androidx.lifecycle.ViewModel() {
        lateinit var settings : CustomTabViewModel.Settings
            private set

        lateinit var tags: List<TagAndUsers>
            private set

        lateinit var states: BooleanArray
            private set

        fun init(set: CustomTabViewModel.Settings, tags: List<TagAndUsers>) {
            this.settings = set.copy()
            this.tags = tags
            this.states = tags.map { t -> set.activeTags.any { it.userTag.id == t.userTag.id } }.toBooleanArray()
        }

        fun setTagState(position: Int, checked: Boolean) {
            states[position] = checked
        }

        fun apply(
            activeNoCommentBookmarks: Boolean? = null,
            activeMutedBookmarks: Boolean? = null,
            activeUnaffiliatedUsers: Boolean? = null
        ) {
            settings =
                settings.copy(
                    activeNoCommentBookmarks = activeNoCommentBookmarks ?: settings.activeNoCommentBookmarks,
                    activeMutedBookmarks = activeMutedBookmarks ?: settings.activeMutedBookmarks,
                    activeUnaffiliatedUsers = activeUnaffiliatedUsers ?: settings.activeUnaffiliatedUsers
                )
        }

        fun createResult() =
            settings.copy(
                activeTags = tags.filterIndexed { idx, _ -> states[idx] }
            )
    }
}
