package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.DialogTitleUserBinding
import com.suihan74.satena.databinding.ListviewItemNotices2Binding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.scenes.bookmarks.dialog.ReportDialog
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.extensions.ContextExtensions.showToast

class NoticeMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(notice: Notice) = NoticeMenuDialog().withArguments {
            putObject(ARG_NOTICE, notice)
        }

        /** 通知 */
        private const val ARG_NOTICE = "ARG_NOTICE"

        /** ユーザーに対する操作を決定するダイアログ用タグ */
        const val DIALOG_NOTICE_USER_MENU = "NoticeMenuDialog.DIALOG_NOTICE_USER_MENU"
    }

    // ------ //

    val viewModel by lazyProvideViewModel {
        DialogViewModel(requireArguments())
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val notice = viewModel.notice

        val titleViewBinding = ListviewItemNotices2Binding.inflate(localLayoutInflater(), null, false).also {
            it.notice = notice
            it.lifecycleOwner = this
        }

        val items: List<Pair<String, () -> Unit>> = notice.users
            .map {
                "id:$it" to {
                    val userMenuDialog = NoticeUserMenuDialog.createInstance(it)
                    userMenuDialog.showAllowingStateLoss(
                        parentFragmentManager,
                        DIALOG_NOTICE_USER_MENU
                    )
                }
            }
            .plus(
                getString(R.string.menu_notice_delete) to { viewModel.onDeleteNoticeListener?.invoke(notice) }
            )

        return createBuilder()
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                items[which].second.invoke()
            }
            .create()
    }

    // ------ //

    /** 通知削除時のイベントリスナを設定 */
    fun setOnDeleteNoticeListener(listener: Listener<Notice>?) = lifecycleScope.launchWhenCreated {
        viewModel.onDeleteNoticeListener = listener
    }

    // ------ //

    class DialogViewModel(args : Bundle) : ViewModel() {
        /** 対象の通知 */
        val notice by lazy { args.getObject<Notice>(ARG_NOTICE)!! }

        /** 通知削除時の処理 */
        var onDeleteNoticeListener: Listener<Notice>? = null
    }

    // ------ //

    /** 通知に含まれるユーザーに対する操作 */
    class NoticeUserMenuDialog : DialogFragment() {
        companion object {
            fun createInstance(user: String) = NoticeUserMenuDialog().withArguments {
                putString(ARG_USER, user)
            }

            /** 対象ユーザー名 */
            private const val ARG_USER = "ARG_USER"

            /** 通報ダイアログ用のタグ */
            const val DIALOG_REPORT = "NoticeUserMenuDialog.DIALOG_REPORT"
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val arguments = requireArguments()
            val user = arguments.getString(ARG_USER)!!

            val titleViewBinding = DialogTitleUserBinding.inflate(localLayoutInflater(), null, false).also {
                it.userName = user
                it.iconUrl = HatenaClient.getUserIconUrl(user)
            }

            val items = arrayOf<Pair<Int, (String) -> Unit>>(
                R.string.bookmark_show_user_entries to { u -> showBookmarkedEntries(u) },
                R.string.bookmark_report to { u -> showReportDialog(u) }
            )

            return createBuilder()
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
            ReportDialog.createInstance(
                user = user,
                userIconUrl = HatenaClient.getUserIconUrl(user)
            ).also { dialog ->
                dialog.setOnReportBookmark { model ->
                    val isSuccess = reportUser(model)
                    val ignoreAfterReporting = model.ignoreAfterReporting

                    if (isSuccess && ignoreAfterReporting) {
                        val result = runCatching { ignoreUser(user) }
                        val context = SatenaApplication.instance
                        if (result.isSuccess) {
                            context.showToast(R.string.msg_report_and_ignore_succeeded, user)
                        }
                        else {
                            context.showToast(R.string.msg_ignore_user_failed, user)
                            Log.e("ignoreUser", "failed: user = $user")
                            result.exceptionOrNull()?.printStackTrace()
                        }
                        return@setOnReportBookmark result.isSuccess
                    }
                    else {
                        return@setOnReportBookmark isSuccess
                    }
                }
                dialog.showAllowingStateLoss(parentFragmentManager, DIALOG_REPORT)
            }
        }

        private suspend fun reportUser(model: ReportDialog.Model): Boolean {
            val result = runCatching {
                HatenaClient.reportAsync(
                    user = model.user,
                    category = model.category,
                    text = model.comment
                ).await()
            }
            return result.getOrDefault(false)
        }

        private suspend fun ignoreUser(user: String) {
            HatenaClient.ignoreUserAsync(user).await()
        }
    }
}
