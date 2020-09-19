package com.suihan74.satena.scenes.bookmarks2.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.whenStarted
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Star
import com.suihan74.hatenaLib.StarsEntry
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.setCustomTitle
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments

class BookmarkMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(
            bookmark: Bookmark,
            starsEntry: StarsEntry?,
            ignored: Boolean,
            userSignedIn: String?
        ) = BookmarkMenuDialog().withArguments {
            putObject(ARG_BOOKMARK, bookmark)
            putObject(ARG_STARS_ENTRY, starsEntry)
            putBoolean(ARG_IGNORED, ignored)
            putString(ARG_USER_SIGNED_IN, userSignedIn)
        }

        private const val ARG_BOOKMARK = "ARG_BOOKMARK"
        private const val ARG_STARS_ENTRY = "ARG_STARS_ENTRY"
        private const val ARG_IGNORED = "ARG_IGNORED"
        private const val ARG_USER_SIGNED_IN = "ARG_USER_SIGNED_IN"
    }

    private val viewModel: DialogViewModel by lazy {
        ViewModelProvider(this)[DialogViewModel::class.java]
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()

        val bookmark = args.getObject<Bookmark>(ARG_BOOKMARK)!!

        val ignored = args.getBoolean(ARG_IGNORED, false)

        val userSignedIn = args.getString(ARG_USER_SIGNED_IN)
        val signedIn = !userSignedIn.isNullOrBlank()

        val starsEntry = args.getObject<StarsEntry>(ARG_STARS_ENTRY)
        val userStars = starsEntry?.allStars?.filter { it.user == userSignedIn } ?: emptyList()

        val titleView = LayoutInflater.from(context).inflate(R.layout.dialog_title_bookmark, null).apply {
            setCustomTitle(bookmark)
        }

        val items = buildList {
            add(R.string.bookmark_show_user_entries to { viewModel.onShowEntries?.invoke(bookmark.user) })
            if (signedIn) {
                if (ignored) {
                    add(R.string.bookmark_unignore to { viewModel.onUnignoreUser?.invoke(bookmark.user) })
                }
                else {
                    add(R.string.bookmark_ignore to { viewModel.onIgnoreUser?.invoke(bookmark.user) })
                }

                if (bookmark.comment.isNotBlank() || bookmark.tags.isNotEmpty()) {
                    add(R.string.bookmark_report to { viewModel.onReportBookmark?.invoke(bookmark) })
                }
            }
            add(R.string.bookmark_user_tags to { viewModel.onSetUserTag?.invoke(bookmark.user) })

            if (userStars.isNotEmpty()) {
                add(R.string.bookmark_delete_star to { viewModel.onDeleteStar?.invoke(bookmark to userStars) })
            }
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setCustomTitle(titleView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(items.map { getString(it.first) }.toTypedArray()) { _, which ->
                items[which].second()
            }
            .create()
    }

    suspend fun setOnShowEntries(listener: Listener<String>?) = whenStarted {
        viewModel.onShowEntries = listener
    }

    suspend fun setOnIgnoreUser(listener: Listener<String>?) = whenStarted {
        viewModel.onIgnoreUser = listener
    }

    suspend fun setOnUnignoreUser(listener: Listener<String>?) = whenStarted {
        viewModel.onUnignoreUser = listener
    }

    suspend fun setOnReportBookmark(listener: Listener<Bookmark>?) = whenStarted {
        viewModel.onReportBookmark = listener
    }

    suspend fun setOnSetUserTag(listener: Listener<String>?) = whenStarted {
        viewModel.onSetUserTag = listener
    }

    suspend fun setOnDeleteStar(listener: Listener<Pair<Bookmark, List<Star>>>?) = whenStarted {
        viewModel.onDeleteStar = listener
    }

    class DialogViewModel : ViewModel() {
        /** ユーザーが最近ブクマしたエントリ一覧を表示する */
        var onShowEntries: Listener<String>? = null

        /** ユーザーを非表示にする */
        var onIgnoreUser: Listener<String>? = null

        /** ユーザーの非表示を解除する */
        var onUnignoreUser: Listener<String>? = null

        /** ブクマを通報する */
        var onReportBookmark: Listener<Bookmark>? = null

        /** ユーザータグをつける */
        var onSetUserTag: Listener<String>? = null

        /** スターを取り消す */
        var onDeleteStar: Listener<Pair<Bookmark, List<Star>>>? = null
    }
}
