package com.suihan74.satena.scenes.preferences.userTag

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.models.userTag.User
import com.suihan74.utilities.SuspendListener
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** タグ付けされたユーザーに対するメニュー */
class UserMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(targetUser: User) = UserMenuDialog().withArguments {
            putObject(ARG_TARGET_USER, targetUser)
        }
        private const val ARG_TARGET_USER = "ARG_TARGET_USER"
    }

    private val viewModel : DialogViewModel by lazy {
        provideViewModel(this) {
            val args = requireArguments()
            DialogViewModel(
                args.getObject<User>(ARG_TARGET_USER)!!
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val labels = viewModel.items.map { getString(it.first) }.toTypedArray()

        return createBuilder()
            .setTitle(viewModel.targetUser.name)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(labels, null)
            .show()
            .apply {
                // アイテムを選択した瞬間にダイアログを閉じないようにする
                // 自動で閉じてしまうと、処理完了前にコルーチンがキャンセルされてしまう可能性が高くなる
                listView.setOnItemClickListener { adapterView, view, i, l ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        viewModel.invokeListener(i)
                        dismiss()
                    }
                }
            }
    }

    suspend fun setOnShowBookmarksListener(listener: SuspendListener<User>?) = whenStarted {
        viewModel.onShowBookmarks = listener
    }

    suspend fun setOnDeleteListener(listener: SuspendListener<User>?) = whenStarted {
        viewModel.onDelete = listener
    }

    // ------ //

    class DialogViewModel(
        /** 捜査対象のユーザー */
        val targetUser: User
    ) : ViewModel() {
        /** メニュー項目 */
        val items = listOf(
            R.string.pref_user_tags_user_menu_show_entries to { onShowBookmarks },
            R.string.pref_user_tags_user_menu_remove to { onDelete }
        )

        var onShowBookmarks: SuspendListener<User>? = null

        var onDelete: SuspendListener<User>? = null

        // itemsの場合クリックしてすぐダイアログが閉じるせいで
        // viewModelScopeやlifecycleScopeを使っていると
        // 途中でキャンセルされる可能性がある
        suspend fun invokeListener(which: Int) {
            val listener = items[which].second()
            listener?.invoke(targetUser)
        }
    }
}
