package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.DialogTitleUserBinding
import com.suihan74.satena.databinding.ListviewItemNotices2Binding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.NoticeTimestamp
import com.suihan74.satena.models.NoticesKey
import com.suihan74.satena.scenes.bookmarks.dialog.ReportDialog
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.showAllowingStateLoss

class NoticeMenuDialog : AlertDialogFragment() {
    companion object {
        fun createInstance(notice: Notice) = NoticeMenuDialog().withArguments {
            putObject(ARG_NOTICE, notice)
        }

        /** 通知 */
        private const val ARG_NOTICE = "ARG_NOTICE"

        /** ユーザーに対する操作を決定するダイアログ用タグ */
        const val DIALOG_NOTICE_USER_MENU = "NoticeMenuDialog.DIALOG_NOTICE_USER_MENU"
    }

    /** 通知削除時のイベントリスナ */
    private var onNoticeRemovedListener: com.suihan74.utilities.Listener<Notice>? = null

    /** 通知削除時のイベントリスナを設定 */
    fun setOnNoticeRemovedListener(listener: com.suihan74.utilities.Listener<Notice>?) {
        onNoticeRemovedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = requireArguments()
        val notice = arguments.getObject<Notice>(ARG_NOTICE)!!
        val users = notice.users

        val titleViewBinding = DataBindingUtil.inflate<ListviewItemNotices2Binding>(
            LayoutInflater.from(ContextThemeWrapper(requireContext(), R.style.AppTheme_Light)),
            R.layout.listview_item_notices2,
            null,
            false
        ).also {
            it.notice = notice
        }

        val items: List<Pair<String, () -> Unit>> = users
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
                getString(R.string.menu_notice_remove) to { removeNotice(notice) }
            )

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                items[which].second.invoke()
            }
            .create()
    }

    /** 通知を削除 */
    private fun removeNotice(notice: Notice) {
        val prefs = SafeSharedPreferences.create<NoticesKey>(context)
        val removedNotices = prefs.get<List<NoticeTimestamp>>(NoticesKey.REMOVED_NOTICE_TIMESTAMPS)
            .plus(NoticeTimestamp(notice.created, notice.modified))

        prefs.edit {
            put(NoticesKey.REMOVED_NOTICE_TIMESTAMPS, removedNotices)
        }

        onNoticeRemovedListener?.invoke(notice)
    }

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

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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

            val items = arrayOf<Pair<Int, (String) -> Unit>>(
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
