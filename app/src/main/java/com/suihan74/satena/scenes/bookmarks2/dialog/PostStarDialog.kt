package com.suihan74.satena.scenes.bookmarks2.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Star
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.utilities.getEnum
import com.suihan74.utilities.getObject
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.putObject

/** 送信するのか削除するのか */
enum class StarDialogMode {
    POST,
    DELETE
}

class PostStarDialog : DialogFragment() {
    companion object {
        fun createInstance(bookmark: Bookmark, starColor: StarColor, quote: String = "") = PostStarDialog().apply {
            arguments = Bundle().apply {
                putObject(ARG_BOOKMARK, bookmark)
                putEnum(ARG_STAR_COLOR, starColor)
                putString(ARG_QUOTE, quote)
                putEnum(ARG_MODE, StarDialogMode.POST)
            }
        }

        fun createInstanceDeleteMode(bookmark: Bookmark, star: Star) = PostStarDialog().apply {
            arguments = Bundle().apply {
                putObject(ARG_BOOKMARK, bookmark)
                putObject(ARG_STAR, star)
                putEnum(ARG_MODE, StarDialogMode.DELETE)
            }
        }

        private const val ARG_MODE = "ARG_MODE"
        private const val ARG_BOOKMARK = "ARG_BOOKMARK"
        private const val ARG_STAR_COLOR = "ARG_STAR_COLOR"
        private const val ARG_QUOTE = "ARG_QUOTE"
        /** deleteモードで使用 */
        private const val ARG_STAR = "ARG_STAR"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()

        val bookmark = args.getObject<Bookmark>(ARG_BOOKMARK)!!
        val starColor = args.getEnum(ARG_STAR_COLOR, StarColor.Yellow)
        val quote = args.getString(ARG_QUOTE, "")
        val star = args.getObject<Star>(ARG_STAR)
        val mode = args.getEnum(ARG_MODE, StarDialogMode.POST)

        val message = when (mode) {
            StarDialogMode.POST -> getString(R.string.msg_post_star_dialog, starColor.name)
            StarDialogMode.DELETE -> getString(R.string.msg_delete_star_dialog)
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.confirm_dialog_title_simple)
            .setIcon(R.drawable.ic_baseline_help)
            .setMessage(message)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                val activity = requireActivity() as BookmarksActivity
                val listener = activity.viewModel as Listener
                when (mode) {
                    StarDialogMode.POST -> listener.onPostStar(this, bookmark, starColor, quote)
                    StarDialogMode.DELETE -> listener.onDeleteStar(this, bookmark, star!!)
                }
            }
            .create()
    }

    /** ダイアログ操作を処理するリスナ */
    interface Listener {
        /** スターを送信する */
        fun onPostStar(dialog: PostStarDialog, bookmark: Bookmark, starColor: StarColor, quote: String)

        /** スターを削除する */
        fun onDeleteStar(dialog: PostStarDialog, bookmark: Bookmark, star: Star)
    }
}
