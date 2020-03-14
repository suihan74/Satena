package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemNotices2Binding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.utilities.users
import com.suihan74.utilities.withArguments

// TODO: やろうとしてちからつきた
class NoticeMenuDialog : AlertDialogFragment() {
    companion object {
        fun createInstance(notice: Notice) = NoticeMenuDialog().withArguments {
            putSerializable(ARG_NOTICE, notice)
        }

        /** 通知に含まれるユーザー名のArray */
        private const val ARG_NOTICE = "ARG_NOTICE"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = requireArguments()
        val notice = arguments.getSerializable(ARG_NOTICE) as Notice
        val users = notice.users.map { "id:$it" }.toTypedArray()

        val titleViewBinding = DataBindingUtil.inflate<ListviewItemNotices2Binding>(
            LayoutInflater.from(ContextThemeWrapper(requireContext(), R.style.AppTheme_Light)),
            R.layout.listview_item_notices2,
            null,
            false
        ).apply {
            this.notice = notice
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(users, null)
            .create()
    }
}
