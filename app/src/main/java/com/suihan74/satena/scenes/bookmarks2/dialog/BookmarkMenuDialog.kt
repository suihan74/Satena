package com.suihan74.satena.scenes.bookmarks2.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.setCustomTitle

class BookmarkMenuDialog : DialogFragment() {
    private lateinit var bookmark : Bookmark

    companion object {
        fun createInstance(bookmark: Bookmark) = BookmarkMenuDialog().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_BOOKMARK, bookmark)
            }
        }

        private const val ARG_BOOKMARK = "ARG_BOOKMARK"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bookmark = requireArguments().getSerializable(ARG_BOOKMARK) as Bookmark

        val titleView = LayoutInflater.from(context).inflate(R.layout.dialog_title_bookmark, null).apply {
            setCustomTitle(bookmark)
        }

        val listener = parentFragment as? Listener
            ?: activity as? Listener

        val ignoreAction =
            if (listener?.isIgnored(bookmark.user) == true) {
                R.string.bookmark_unignore to { listener?.onIgnoreUser(bookmark.user, false) }
            }
            else {
                R.string.bookmark_ignore to { listener?.onIgnoreUser(bookmark.user, true) }
            }

        val items = listOf(
            R.string.bookmark_show_user_entries to { listener?.onShowEntries(bookmark.user) },
            ignoreAction,
            R.string.bookmark_report to { listener?.onReportBookmark(bookmark) },
            R.string.bookmark_user_tags to { listener?.onSetUserTag(bookmark.user) }
        )

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setCustomTitle(titleView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(items.map { getString(it.first) }.toTypedArray()) { _, which ->
                items[which].second()
            }
            .create()
    }

    /** ダイアログ操作を処理するリスナ */
    interface Listener {
        /** ユーザーが既に非表示かを確認する */
        fun isIgnored(user: String) : Boolean
        /** ユーザーが最近ブクマしたエントリ一覧を表示する */
        fun onShowEntries(user: String)
        /** ユーザーの非表示状態を設定する */
        fun onIgnoreUser(user: String, ignore: Boolean)
        /** ブクマを通報する */
        fun onReportBookmark(bookmark: Bookmark)
        /** ユーザータグをつける */
        fun onSetUserTag(user: String)
    }
}
