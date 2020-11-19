package com.suihan74.satena.scenes.bookmarks.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.whenStarted
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.*

class PostStarDialog : DialogFragment() {
    companion object {
        fun createInstance(
            bookmark: Bookmark,
            starColor: StarColor,
            quote: String = ""
        ) = PostStarDialog().withArguments {
            putObject(ARG_BOOKMARK, bookmark)
            putEnum(ARG_STAR_COLOR, starColor)
            putString(ARG_QUOTE, quote)
        }

        private const val ARG_BOOKMARK = "ARG_BOOKMARK"
        private const val ARG_STAR_COLOR = "ARG_STAR_COLOR"
        private const val ARG_QUOTE = "ARG_QUOTE"
    }

    private val viewModel: DialogViewModel by lazy {
        ViewModelProvider(this)[DialogViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()

        val bookmark = args.getObject<Bookmark>(ARG_BOOKMARK)!!
        val starColor = args.getEnum(ARG_STAR_COLOR, StarColor.Yellow)
        val quote = args.getString(ARG_QUOTE, "")

        val message = getString(R.string.msg_post_star_dialog, starColor.name)

        return createBuilder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setIcon(R.drawable.ic_baseline_help)
            .setMessage(message)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.onPostStar?.invoke(Triple(bookmark, starColor, quote))
            }
            .create()
    }

    suspend fun setOnPostStar(listener: Listener<Triple<Bookmark, StarColor, String>>?) = whenStarted {
        viewModel.onPostStar = listener
    }

    // ------ //

    class DialogViewModel : ViewModel() {
        var onPostStar: Listener<Triple<Bookmark, StarColor, String>>? = null
    }
}
