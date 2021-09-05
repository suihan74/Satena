package com.suihan74.satena.scenes.entries2

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.createIntentWithoutThisApplication
import com.suihan74.utilities.extensions.putObjectExtra
import kotlinx.coroutines.launch

/** コメントに対するメニュー項目 */
interface CommentMenuActions {

    /** コメントを開く */
    fun openComment(activity: FragmentActivity, entry: Entry, bookmarkResult: BookmarkResult)

    /** アプリ内ブラウザで開く */
    fun openCommentInnerBrowser(activity: FragmentActivity, bookmarkResult: BookmarkResult)

    /** 外部アプリで開く */
    fun openCommentIntent(activity: FragmentActivity, bookmarkResult: BookmarkResult)

    /** 最近のブックマークを見る */
    fun openCommentUserEntries(activity: FragmentActivity, bookmarkResult: BookmarkResult)

    /** ブコメを修正する */
    fun modifyComment(activity: FragmentActivity, entry: Entry, bookmarkResult: BookmarkResult)

    /** ブコメを削除する */
    fun deleteComment(activity: FragmentActivity, entry: Entry, bookmarkResult: BookmarkResult)

    // ------ //

    /** コメントに対するメニューダイアログを開く */
    @OptIn(ExperimentalStdlibApi::class)
    fun openCommentMenuDialog(
        entry: Entry,
        bookmarkResult: BookmarkResult,
        fragmentManager: FragmentManager
    ) {
        val items = buildList<Pair<Int, (DialogFragment)->Unit>> {
            if (bookmarkResult.eid != null) {
                add(R.string.entry_comment_action_open to { f -> openComment(f.requireActivity(), entry, bookmarkResult) })
            }
            add(R.string.entry_comment_action_open_browser to { f -> openCommentInnerBrowser(f.requireActivity(), bookmarkResult) })
            add(R.string.entry_comment_action_open_app to { f -> openCommentIntent(f.requireActivity(), bookmarkResult) })
            if (bookmarkResult.eid != null) {
                add(R.string.entry_comment_action_show_user_entries to { f -> openCommentUserEntries(f.requireActivity(), bookmarkResult) })
            }

            val userSignedIn = HatenaClient.account?.name.orEmpty()
            if (bookmarkResult.user == userSignedIn && bookmarkResult.eid != null) {
                add(R.string.entry_comment_action_modify_bookmark to { f -> modifyComment(f.requireActivity(), entry, bookmarkResult) })
                add(R.string.entry_comment_action_delete_bookmark to { f -> deleteComment(f.requireActivity(), entry, bookmarkResult) })
            }
        }
        val labels = items.map { it.first }

        AlertDialogFragment.Builder()
            .setTitle(bookmarkResult.user)
            .setItems(labels) { f, which -> items[which].second.invoke(f) }
            .setNegativeButton(R.string.dialog_cancel)
            .create()
            .show(fragmentManager, null)
    }
}

// ------ //

/** コメントに対する各処理の実装 */
class CommentMenuActionsImpl(
    private val repository: EntriesRepository
) : CommentMenuActions {
    /** コメントを開く */
    override fun openComment(activity: FragmentActivity, entry: Entry, bookmarkResult: BookmarkResult) {
        val intent = Intent(activity, BookmarksActivity::class.java).apply {
            putObjectExtra(BookmarksActivity.EXTRA_ENTRY, entry)
            putExtra(BookmarksActivity.EXTRA_TARGET_USER, bookmarkResult.user)
        }
        activity.startActivity(intent)
    }

    /** アプリ内ブラウザで開く */
    override fun openCommentInnerBrowser(activity: FragmentActivity, bookmarkResult: BookmarkResult) {
        activity.startInnerBrowser(url = bookmarkResult.permalink)
    }

    /** 外部アプリで開く */
    override fun openCommentIntent(activity: FragmentActivity, bookmarkResult: BookmarkResult) {
        val intent = Intent().let {
            it.action = Intent.ACTION_VIEW
            it.data = Uri.parse(bookmarkResult.permalink)
            it.createIntentWithoutThisApplication(activity, title = bookmarkResult.permalink)
        }
        activity.startActivity(intent)
    }

    /** 最近のブックマークを見る */
    override fun openCommentUserEntries(activity: FragmentActivity, bookmarkResult: BookmarkResult) {
        activity.alsoAs<EntriesActivity> {
            it.showUserEntries(bookmarkResult.user)
        }
    }

    /** ブコメを修正する */
    override fun modifyComment(activity: FragmentActivity, entry: Entry, bookmarkResult: BookmarkResult) {
        val intent = Intent(activity, BookmarkPostActivity::class.java).also {
            it.putObjectExtra(BookmarkPostActivity.EXTRA_ENTRY, entry)
        }
        activity.startActivityForResult(
            intent,
            BookmarkPostActivity.REQUEST_CODE
        )
    }

    /** ブコメを削除する */
    override fun deleteComment(activity: FragmentActivity, entry: Entry, bookmarkResult: BookmarkResult) {
        activity.lifecycleScope.launch {
            val result = runCatching {
                repository.deleteBookmark(entry)
            }
            if (result.isSuccess) {
                activity.alsoAs<EntriesActivity> { a ->
                    a.removeBookmark(entry)
                }
                activity.showToast(R.string.msg_remove_bookmark_succeeded)
            }
            else {
                activity.showToast(R.string.msg_remove_bookmark_failed)
            }
        }
    }
}
