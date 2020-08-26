package com.suihan74.satena.scenes.bookmarks2.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.setCustomTitle
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.utilities.getObject
import com.suihan74.utilities.putObject

class BookmarkMenuDialog : DialogFragment() {
    private lateinit var bookmark : Bookmark

    companion object {
        fun createInstance(bookmark: Bookmark, signedIn: Boolean?) = BookmarkMenuDialog().apply {
            arguments = Bundle().apply {
                putObject(ARG_BOOKMARK, bookmark)
                putBoolean(ARG_SIGNED_IN, signedIn == true)
            }
        }

        private const val ARG_BOOKMARK = "ARG_BOOKMARK"
        private const val ARG_SIGNED_IN = "ARG_SIGNED_IN"
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val listener = parentFragment as? Listener ?: activity as? Listener
        val activity = requireActivity() as BookmarksActivity
        val listener = activity.viewModel as Listener
        bookmark = requireArguments().getObject<Bookmark>(ARG_BOOKMARK)!!

        val titleView = LayoutInflater.from(context).inflate(R.layout.dialog_title_bookmark, null).apply {
            setCustomTitle(bookmark)
        }

        val signedIn = requireArguments().getBoolean(ARG_SIGNED_IN)

        val items = buildList {
            val dialog = this@BookmarkMenuDialog
            add(R.string.bookmark_show_user_entries to { listener.onShowEntries(dialog, bookmark.user) })
            if (signedIn) {
                if (listener.isIgnored(dialog, bookmark.user)) {
                    add(R.string.bookmark_unignore to { listener.onIgnoreUser(dialog, bookmark.user, false) })
                }
                else {
                    add(R.string.bookmark_ignore to { listener.onIgnoreUser(dialog, bookmark.user, true) })
                }

                if (bookmark.comment.isNotBlank() || bookmark.tags.isNotEmpty()) {
                    add(R.string.bookmark_report to { listener.onReportBookmark(dialog, bookmark) })
                }
            }
            add(R.string.bookmark_user_tags to { listener.onSetUserTag(dialog, bookmark.user) })
        }

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
        fun isIgnored(dialog: BookmarkMenuDialog, user: String) : Boolean
        /** ユーザーが最近ブクマしたエントリ一覧を表示する */
        fun onShowEntries(dialog: BookmarkMenuDialog, user: String)
        /** ユーザーの非表示状態を設定する */
        fun onIgnoreUser(dialog: BookmarkMenuDialog, user: String, ignore: Boolean)
        /** ブクマを通報する */
        fun onReportBookmark(dialog: BookmarkMenuDialog, bookmark: Bookmark)
        /** ユーザータグをつける */
        fun onSetUserTag(dialog: BookmarkMenuDialog, user: String)
    }
}
