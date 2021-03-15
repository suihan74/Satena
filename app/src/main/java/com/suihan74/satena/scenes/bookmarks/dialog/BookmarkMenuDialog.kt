package com.suihan74.satena.scenes.bookmarks.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Star
import com.suihan74.hatenaLib.StarsEntry
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.dialogs.setCustomTitle
import com.suihan74.utilities.DialogListener
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel
import org.threeten.bp.LocalDateTime

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

    private val viewModel by lazyProvideViewModel {
        DialogViewModel(requireArguments())
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = localLayoutInflater()
        val titleView = inflater.inflate(
            R.layout.dialog_title_bookmark,
            null
        ).also {
            it.setCustomTitle(viewModel.bookmark)
        }

        return createBuilder()
            .setCustomTitle(titleView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(viewModel.createLabels(requireContext())) { _, which ->
                viewModel.invokeAction(which, this)
            }
            .create()
    }

    fun setOnShowEntries(listener: DialogListener<String>?) = lifecycleScope.launchWhenCreated {
        viewModel.onShowEntries = listener
    }

    fun setOnShowCommentEntry(listener: DialogListener<Bookmark>?) = lifecycleScope.launchWhenCreated {
        viewModel.onShowCommentEntry = listener
    }

    fun setOnShareCommentPageUrl(listener: DialogListener<Bookmark>?) = lifecycleScope.launchWhenCreated {
        viewModel.onShareCommentPageUrl = listener
    }

    fun setOnIgnoreUser(listener: DialogListener<String>?) = lifecycleScope.launchWhenCreated {
        viewModel.onIgnoreUser = listener
    }

    fun setOnUnignoreUser(listener: DialogListener<String>?) = lifecycleScope.launchWhenCreated {
        viewModel.onUnignoreUser = listener
    }

    fun setOnReportBookmark(listener: DialogListener<Bookmark>?) = lifecycleScope.launchWhenCreated {
        viewModel.onReportBookmark = listener
    }

    fun setOnSetUserTag(listener: DialogListener<String>?) = lifecycleScope.launchWhenCreated {
        viewModel.onSetUserTag = listener
    }

    fun setOnDeleteStar(listener: DialogListener<Pair<Bookmark, List<Star>>>?) = lifecycleScope.launchWhenCreated {
        viewModel.onDeleteStar = listener
    }

    fun setOnDeleteBookmark(listener: DialogListener<Bookmark>?) = lifecycleScope.launchWhenCreated {
        viewModel.onDeleteBookmark = listener
    }

    // ------ //

    class DialogViewModel(args: Bundle) : ViewModel() {
        val bookmark = args.getObject<Bookmark>(ARG_BOOKMARK)!!

        val ignored = args.getBoolean(ARG_IGNORED, false)

        val userSignedIn = args.getString(ARG_USER_SIGNED_IN)

        val starsEntry = args.getObject<StarsEntry>(ARG_STARS_ENTRY)

        /** サインイン状態 */
        val signedIn : Boolean =
            !userSignedIn.isNullOrBlank()

        /** ユーザーが付けたスター */
        val userStars =
            starsEntry?.allStars?.filter { it.user == userSignedIn } ?: emptyList()

        // ------ //

        /** ユーザーが最近ブクマしたエントリ一覧を表示する */
        var onShowEntries: DialogListener<String>? = null

        /** ブコメへのブックマークページを開く */
        var onShowCommentEntry: DialogListener<Bookmark>? = null

        /** ブコメのコメントページURLを「共有」する */
        var onShareCommentPageUrl: DialogListener<Bookmark>? = null

        /** ユーザーを非表示にする */
        var onIgnoreUser: DialogListener<String>? = null

        /** ユーザーの非表示を解除する */
        var onUnignoreUser: DialogListener<String>? = null

        /** ブクマを通報する */
        var onReportBookmark: DialogListener<Bookmark>? = null

        /** ユーザータグをつける */
        var onSetUserTag: DialogListener<String>? = null

        /** スターを取り消す */
        var onDeleteStar: DialogListener<Pair<Bookmark, List<Star>>>? = null

        /** (自分のブクマを)削除する */
        var onDeleteBookmark: DialogListener<Bookmark>? = null

        // ------ //

        /** メニュー項目 */
        @OptIn(ExperimentalStdlibApi::class)
        val items by lazy {
                // TODO: ダミーの判定方法は変えた方がいいかもしれない
                val dummyBookmark = bookmark.timestamp == LocalDateTime.MIN

            buildList<Pair<Int, (BookmarkMenuDialog)->Unit>> {
                add(R.string.bookmark_show_user_entries to { onShowEntries?.invoke(bookmark.user, it) })
                add(R.string.bookmark_show_comment_entry to { onShowCommentEntry?.invoke(bookmark, it) })
                add(R.string.bookmark_share_comment_page_url to { onShareCommentPageUrl?.invoke(bookmark, it) })

                if (signedIn) {
                    if (ignored) {
                        add(R.string.bookmark_unignore to { onUnignoreUser?.invoke(bookmark.user, it) })
                    }
                    else {
                        add(R.string.bookmark_ignore to { onIgnoreUser?.invoke(bookmark.user, it) })
                    }

                    if (!dummyBookmark && (bookmark.comment.isNotBlank() || bookmark.tags.isNotEmpty())) {
                        add(R.string.bookmark_report to { onReportBookmark?.invoke(bookmark, it) })
                    }
                }
                add(R.string.bookmark_user_tags to { onSetUserTag?.invoke(bookmark.user, it) })

                if (userStars.isNotEmpty()) {
                    add(R.string.bookmark_delete_star to { onDeleteStar?.invoke(bookmark to userStars, it) })
                }

                if (userSignedIn == bookmark.user && !dummyBookmark) {
                    add(R.string.bookmark_delete to { onDeleteBookmark?.invoke(bookmark, it) })
                }
            }
        }

        /** メニューラベル */
        fun createLabels(context: Context) =
            items.map { context.getString(it.first) }.toTypedArray()

        /** メニューアクションを実行 */
        fun invokeAction(which: Int, dialogFragment: BookmarkMenuDialog) {
            items.getOrNull(which)?.second?.invoke(dialogFragment)
        }
    }
}
