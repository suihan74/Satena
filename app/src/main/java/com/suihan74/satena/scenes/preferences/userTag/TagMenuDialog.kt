package com.suihan74.satena.scenes.preferences.userTag

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.utilities.SuspendListener
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** タグ付けされたユーザーに対するメニュー */
class TagMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(targetTag: Tag) = TagMenuDialog().withArguments {
            putObject(ARG_TARGET_TAG, targetTag)
        }
        private const val ARG_TARGET_TAG = "ARG_TARGET_TAG"
    }

    private val viewModel : DialogViewModel by lazyProvideViewModel {
        DialogViewModel(
            requireArguments().getObject<Tag>(ARG_TARGET_TAG)!!
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val labels = viewModel.items.map { getString(it.first) }.toTypedArray()

        return createBuilder()
            .setTitle(viewModel.targetTag.name)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(labels, null)
            .show()
            .apply {
                // アイテムを選択した瞬間にダイアログを閉じないようにする
                // 自動で閉じてしまうと、処理完了前にコルーチンがキャンセルされてしまう可能性が高くなる
                listView.setOnItemClickListener { _, _, i, _ ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        runCatching {
                            viewModel.invokeListener(i, this@TagMenuDialog)
                            dismiss()
                        }
                    }
                }
            }
    }

    fun setOnEditListener(listener: SuspendListener<Pair<Tag, TagMenuDialog>>?) = lifecycleScope.launchWhenCreated {
        viewModel.onEdit = listener
    }

    fun setOnDeleteListener(listener: SuspendListener<Pair<Tag, TagMenuDialog>>?) = lifecycleScope.launchWhenCreated {
        viewModel.onDelete = listener
    }

    // ------ //

    class DialogViewModel(
        /** 操作対象のタグ */
        val targetTag: Tag
    ) : ViewModel() {
        /** メニュー項目 */
        val items = listOf(
            R.string.pref_user_tags_tag_menu_edit to { onEdit },
            R.string.pref_user_tags_tag_menu_remove to { onDelete }
        )

        var onEdit: SuspendListener<Pair<Tag, TagMenuDialog>>? = null

        var onDelete: SuspendListener<Pair<Tag, TagMenuDialog>>? = null

        // itemsの場合クリックしてすぐダイアログが閉じるせいで
        // viewModelScopeやlifecycleScopeを使っていると
        // 途中でキャンセルされる可能性がある
        suspend fun invokeListener(which: Int, dialogFragment: TagMenuDialog) {
            val listener = items[which].second()
            listener?.invoke(targetTag to dialogFragment)
        }
    }
}
