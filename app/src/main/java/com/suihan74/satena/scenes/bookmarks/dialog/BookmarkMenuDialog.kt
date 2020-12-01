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
import com.suihan74.utilities.Listener
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
                viewModel.invokeAction(which)
            }
            .create()
    }

    fun setOnShowEntries(listener: Listener<String>?) = lifecycleScope.launchWhenCreated {
        viewModel.onShowEntries = listener
    }

    fun setOnIgnoreUser(listener: Listener<String>?) = lifecycleScope.launchWhenCreated {
        viewModel.onIgnoreUser = listener
    }

    fun setOnUnignoreUser(listener: Listener<String>?) = lifecycleScope.launchWhenCreated {
        viewModel.onUnignoreUser = listener
    }

    fun setOnReportBookmark(listener: Listener<Bookmark>?) = lifecycleScope.launchWhenCreated {
        viewModel.onReportBookmark = listener
    }

    fun setOnSetUserTag(listener: Listener<String>?) = lifecycleScope.launchWhenCreated {
        viewModel.onSetUserTag = listener
    }

    fun setOnDeleteStar(listener: Listener<Pair<Bookmark, List<Star>>>?) = lifecycleScope.launchWhenCreated {
        viewModel.onDeleteStar = listener
    }

    fun setOnDeleteBookmark(listener: Listener<Bookmark>?) = lifecycleScope.launchWhenCreated {
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

        /** (自分のブクマを)削除する */
        var onDeleteBookmark: Listener<Bookmark>? = null

        // ------ //

        /** メニュー項目 */
        @OptIn(ExperimentalStdlibApi::class)
        val items by lazy {
            buildList {
                // TODO: ダミーの判定方法は変えた方がいいかもしれない
                val dummyBookmark = bookmark.timestamp == LocalDateTime.MIN

                add(R.string.bookmark_show_user_entries to { onShowEntries?.invoke(bookmark.user) })
                if (signedIn) {
                    if (ignored) {
                        add(R.string.bookmark_unignore to { onUnignoreUser?.invoke(bookmark.user) })
                    }
                    else {
                        add(R.string.bookmark_ignore to { onIgnoreUser?.invoke(bookmark.user) })
                    }

                    if (!dummyBookmark && (bookmark.comment.isNotBlank() || bookmark.tags.isNotEmpty())) {
                        add(R.string.bookmark_report to { onReportBookmark?.invoke(bookmark) })
                    }
                }
                add(R.string.bookmark_user_tags to { onSetUserTag?.invoke(bookmark.user) })

                if (userStars.isNotEmpty()) {
                    add(R.string.bookmark_delete_star to { onDeleteStar?.invoke(bookmark to userStars) })
                }

                if (userSignedIn == bookmark.user && !dummyBookmark) {
                    add(R.string.bookmark_delete to { onDeleteBookmark?.invoke(bookmark) })
                }
            }
        }

        /** メニューラベル */
        fun createLabels(context: Context) =
            items.map { context.getString(it.first) }.toTypedArray()

        /** メニューアクションを実行 */
        fun invokeAction(which: Int) {
            items.getOrNull(which)?.second?.invoke()
        }
    }
}
