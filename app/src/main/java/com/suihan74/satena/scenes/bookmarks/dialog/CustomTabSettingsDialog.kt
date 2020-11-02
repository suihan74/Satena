package com.suihan74.satena.scenes.bookmarks.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.utilities.Listener
import com.suihan74.utilities.provideViewModel

/**
 * 「カスタム」タブの表示対象を設定するダイアログ
 */
class CustomTabSettingsDialog : DialogFragment() {

    companion object {
        fun createInstance(
            bookmarksRepository: BookmarksRepository
        ) = CustomTabSettingsDialog().also { instance ->
            instance.lifecycleScope.launchWhenCreated {
                instance.viewModel.repository = bookmarksRepository
            }
        }
    }

    // ------ //

    private val viewModel by lazy {
        provideViewModel(this) {
            DialogViewModel()
        }
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setTitle(R.string.custom_bookmarks_tab_pref_dialog_title)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.invokePositiveAction()
            }
            .setMultiChoiceItems(
                viewModel.createLabels(context),
                viewModel.checkedStates
            ) { _, which, value -> viewModel.checkItem(which, value) }
            .create()
    }

    /** 設定完了後の追加処理をセットする */
    fun setOnCompletedListener(l : Listener<Unit>?) = lifecycleScope.launchWhenCreated {
        viewModel.onCompleted = l
    }

    // ------ //

    class DialogViewModel : ViewModel() {
        /**
         * リポジトリ
         *
         * ```lifecycleScope#launchWhenCreated```によって初期化されるので、
         * リポジトリを参照するプロパティはlazy使うなどして#onCreate後に呼び出されるようにしなければならない
         */
        lateinit var repository : BookmarksRepository

        /** 完了後の処理 */
        var onCompleted : Listener<Unit>? = null

        /** 必ず存在する項目 */
        private val staticItems by lazy {
            listOf(
                MenuItem(
                    R.string.custom_bookmarks_no_comment_active,
                    repository.showNoCommentUsersInCustomBookmarks.value == true,
                ) { _, value ->
                    repository.showNoCommentUsersInCustomBookmarks.value = value
                },

                MenuItem(
                    R.string.custom_bookmarks_ignored_user_active,
                    repository.showMutedUsersInCustomBookmarks.value == true
                ) { _, value ->
                    repository.showMutedUsersInCustomBookmarks.value = value
                },

                MenuItem(
                    R.string.custom_bookmarks_no_user_tags_active,
                    repository.showUnaffiliatedUsersInCustomBookmarks.value == true
                ) { _, value ->
                    repository.showUnaffiliatedUsersInCustomBookmarks.value = value
                }
            )
        }

        /** メニュー項目 */
        @OptIn(ExperimentalStdlibApi::class)
        val items by lazy {
            buildList {
                addAll(staticItems)
            }
        }

        /** チェック状態 */
        val checkedStates by lazy {
            items.map { it.initialChecked }.toBooleanArray()
        }

        // ------ //

        /** 表示用のメニュー項目を作成 */
        fun createLabels(context: Context) =
            items.map { item -> context.getString(item.labelId) }.toTypedArray()

        /** 項目のチェック状態が変化した */
        fun checkItem(which: Int, value: Boolean) {
            checkedStates[which] = value
        }

        /** 登録処理 */
        fun invokePositiveAction() {
            checkedStates.forEachIndexed { idx, b ->
                items[idx].changer(idx, b)
            }
            onCompleted?.invoke(Unit)
        }

        // ------ //

        /** メニュー項目 */
        data class MenuItem(
            /** メニュー表示 */
            @StringRes
            val labelId : Int,

            /** 初期値 */
            val initialChecked : Boolean,

            /** 値の変更 */
            val changer : (which: Int, value: Boolean)->Unit
        )
    }
}
