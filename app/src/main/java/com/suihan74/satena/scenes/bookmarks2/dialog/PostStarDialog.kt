package com.suihan74.satena.scenes.bookmarks2.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.utilities.getEnum
import com.suihan74.utilities.getObject
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.putObject

class PostStarDialog : DialogFragment() {
    private lateinit var bookmark : Bookmark
    private lateinit var starColor : StarColor
    private lateinit var quote : String

    companion object {
        fun createInstance(bookmark: Bookmark, starColor: StarColor, quote: String = "") = PostStarDialog().apply {
            arguments = Bundle().apply {
                putObject(ARG_BOOKMARK, bookmark)
                putEnum(ARG_STAR_COLOR, starColor)
                putString(ARG_QUOTE, quote)
            }
        }

        private const val ARG_BOOKMARK = "ARG_BOOKMARK"
        private const val ARG_STAR_COLOR = "ARG_STAR_COLOR"
        private const val ARG_QUOTE = "ARG_QUOTE"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bookmark = requireArguments().getObject<Bookmark>(ARG_BOOKMARK)!!
        starColor = requireArguments().getEnum(ARG_STAR_COLOR, StarColor.Yellow)
        quote = requireArguments().getString(ARG_QUOTE, "")

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.confirm_dialog_title_simple)
            .setIcon(R.drawable.ic_baseline_help)
            .setMessage(getString(R.string.msg_post_star_dialog, starColor.name))
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                val activity = requireActivity() as BookmarksActivity
                val listener = activity.viewModel as Listener
                listener.onPostStar(this, bookmark, starColor, quote)
            }
            .create()
    }

    /** ダイアログ操作を処理するリスナ */
    interface Listener {
        /** スターを送信する */
        fun onPostStar(dialog: PostStarDialog, bookmark: Bookmark, starColor: StarColor, quote: String)
    }
}
