package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleUserBinding
import com.suihan74.satena.databinding.ListviewItemNotices2Binding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.ReportDialogFragment
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.users
import com.suihan74.utilities.withArguments

class NoticeMenuDialog : AlertDialogFragment() {
    companion object {
        fun createInstance(notice: Notice) = NoticeMenuDialog().withArguments {
            putSerializable(ARG_NOTICE, notice)
        }

        /** 通知 */
        private const val ARG_NOTICE = "ARG_NOTICE"

        /** ユーザーに対する操作を決定するダイアログ用タグ */
        const val DIALOG_NOTICE_USER_MENU = "NoticeMenuDialog.DIALOG_NOTICE_USER_MENU"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        val arguments = requireArguments()
        val notice = arguments.getSerializable(ARG_NOTICE) as Notice
        val users = notice.users

        val titleViewBinding = DataBindingUtil.inflate<ListviewItemNotices2Binding>(
            LayoutInflater.from(ContextThemeWrapper(requireContext(), R.style.AppTheme_Light)),
            R.layout.listview_item_notices2,
            null,
            false
        ).also {
            it.notice = notice
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(users.map { "id:$it" }.toTypedArray()) { _, which ->
                val userMenuDialog = NoticeUserMenuDialog.createInstance(users[which])
                userMenuDialog.show(parentFragmentManager, DIALOG_NOTICE_USER_MENU)
            }
            .create()
    }


    // TODO: ユーザーに対する操作の実装
    /** 通知に含まれるユーザーに対する操作 */
    class NoticeUserMenuDialog : AlertDialogFragment() {
        companion object {
            fun createInstance(user: String) = NoticeUserMenuDialog().withArguments {
                putString(ARG_USER, user)
            }

            /** 対象ユーザー名 */
            private const val ARG_USER = "ARG_USER"

            /** 通報ダイアログ用のタグ */
            const val DIALOG_REPORT = "NoticeUserMenuDialog.DIALOG_REPORT"
        }

        override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
            val arguments = requireArguments()
            val user = arguments.getString(ARG_USER)!!

            val titleViewBinding = DataBindingUtil.inflate<DialogTitleUserBinding>(
                LayoutInflater.from(ContextThemeWrapper(requireContext(), R.style.AppTheme_Light)),
                R.layout.dialog_title_user,
                null,
                false
            ).also {
                it.userName = user
                it.iconUrl = HatenaClient.getUserIconUrl(user)
            }

            val items = arrayOf<Pair<Int, (String)->Unit>>(
                R.string.bookmark_show_user_entries to { u -> showBookmarkedEntries(u) },
                R.string.bookmark_report to { u -> showReportDialog(u) }
            )

            return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
                .setCustomTitle(titleViewBinding.root)
                .setItems(items.map { getString(it.first) }.toTypedArray()) { _, which ->
                    val action = items[which].second
                    action.invoke(user)
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .create()
        }

        /** ユーザーがブクマしたエントリ一覧画面を表示する */
        private fun showBookmarkedEntries(user: String) {
            val activity = requireActivity() as EntriesActivity
            activity.showUserEntries(user)
        }

        /** ユーザーを通報する */
        private fun showReportDialog(user: String) {
            val dialog = ReportDialogFragment.createInstance(user)
            dialog.show(parentFragmentManager, DIALOG_REPORT)
        }
    }
}