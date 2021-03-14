package com.suihan74.satena.scenes.bookmarks.viewModel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.dialog.BookmarkMenuDialog
import com.suihan74.satena.scenes.bookmarks.dialog.ReportDialog
import com.suihan74.satena.scenes.bookmarks.dialog.StarDeletionDialog
import com.suihan74.satena.scenes.bookmarks.dialog.UserTagSelectionDialog
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.exceptions.AlreadyExistedException
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface BookmarkMenuActions {

}

// ------ //

class BookmarkMenuActionsImpl(
    private val repository: BookmarksRepository
) : BookmarkMenuActions {
    private val DIALOG_BOOKMARK_MENU by lazy { "DIALOG_BOOKMARK_MENU" }

    /** ブクマ項目に対する操作メニューを表示 */
    suspend fun openBookmarkMenuDialog(
        entry: Entry,
        bookmark: Bookmark,
        starsEntry: StarsEntry?,
        fragmentManager: FragmentManager,
    ) = withContext(Dispatchers.Main) {
        val dialog = BookmarkMenuDialog.createInstance(
            bookmark,
            starsEntry,
            repository.checkIgnoredUser(bookmark.user),
            repository.userSignedIn
        )

        dialog.setOnShowEntries { user, f -> showEntries(f.requireActivity(), user) }

        dialog.setOnShowCommentEntry { b, f -> showCommentEntry(f.requireActivity(), entry, b) }

        dialog.setOnShareCommentPageUrl { b, f -> shareCommentPageUrl(f.requireActivity(), entry, b) }

        dialog.setOnIgnoreUser { user, f -> ignoreUser(user, f.requireActivity().lifecycleScope) }

        dialog.setOnUnignoreUser { user, f -> unIgnoreUser(user, f.requireActivity().lifecycleScope) }

        dialog.setOnReportBookmark { b, f -> reportBookmark(entry, b, f.parentFragmentManager) }

        dialog.setOnSetUserTag { user, f ->
            openUserTagSelectionDialog(
                user,
                f.parentFragmentManager,
                f.requireActivity().lifecycleScope
            )
        }

        dialog.setOnDeleteStar { (b, stars), f ->
            openDeleteStarDialog(
                entry,
                b,
                stars,
                f.parentFragmentManager
            )
        }

        dialog.setOnDeleteBookmark { b, f ->
            val a = f.requireActivity()
            openConfirmBookmarkDeletionDialog(
                a,
                b,
                f.parentFragmentManager,
                a.lifecycleScope
            )
        }

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_BOOKMARK_MENU)
    }

    /** ユーザーがブクマ済みのエントリ一覧画面を開く */
    private fun showEntries(activity: Activity, user: String) {
        val intent = Intent(activity, EntriesActivity::class.java).apply {
            putExtra(EntriesActivity.EXTRA_USER, user)
        }
        activity.startActivity(intent)
    }

    /**
     * ブクマに対するブクマページを開く
     */
    private fun showCommentEntry(activity: Activity, entry: Entry, bookmark: Bookmark) {
        val intent = Intent(activity, BookmarksActivity::class.java).apply {
            putExtra(BookmarksActivity.EXTRA_ENTRY_URL, bookmark.getCommentPageUrl(entry))
        }
        activity.startActivity(intent)
    }

    /**
     * ブクマのコメントページURLを「共有」する
     */
    private fun shareCommentPageUrl(activity: Activity, entry: Entry, bookmark: Bookmark) {
        val url = bookmark.getCommentPageUrl(entry)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(
            Intent.createChooser(intent, url)
        )
    }

    /** ユーザーを非表示にする */
    private fun ignoreUser(user: String, coroutineScope: CoroutineScope) = coroutineScope.launch(Dispatchers.Main) {
        val result = runCatching {
            repository.ignoreUser(user)
        }

        if (result.isSuccess) {
            repository.refreshBookmarks()
            SatenaApplication.instance.showToast(
                R.string.msg_ignore_user_succeeded,
                user
            )
        }
        else {
            SatenaApplication.instance.showToast(
                R.string.msg_ignore_user_failed,
                user
            )
        }
    }

    /** ユーザーの非表示を解除する */
    private fun unIgnoreUser(user: String, coroutineScope: CoroutineScope) = coroutineScope.launch(Dispatchers.Main) {
        val result = runCatching {
            repository.unIgnoreUser(user)
        }

        if (result.isSuccess) {
            repository.refreshBookmarks()
            SatenaApplication.instance.showToast(
                R.string.msg_unignore_user_succeeded,
                user
            )
        }
        else {
            SatenaApplication.instance.showToast(
                R.string.msg_unignore_user_failed,
                user
            )
        }
    }

    /** ブクマを通報する */
    private fun reportBookmark(
        entry: Entry,
        bookmark: Bookmark,
        fragmentManager: FragmentManager
    ) {
        val dialog = ReportDialog.createInstance(
            user = bookmark.user,
            userIconUrl = bookmark.userIconUrl,
            comment = bookmark.commentRaw
        )

        dialog.setOnReportBookmark { model ->
            val result = runCatching {
                repository.reportBookmark(entry, bookmark, model.category, model)
            }

            val context = SatenaApplication.instance
            if (result.isSuccess) {
                if (model.ignoreAfterReporting) {
                    repository.refreshBookmarks()
                    context.showToast(R.string.msg_report_and_ignore_succeeded, model.user)
                }
                else {
                    context.showToast(R.string.msg_report_succeeded, model.user)
                }
            }
            else {
                Log.e("reportBookmark", Log.getStackTraceString(result.exceptionOrNull()))
                context.showToast(R.string.msg_report_failed)
            }

            return@setOnReportBookmark result.isSuccess
        }

        dialog.showAllowingStateLoss(fragmentManager)
    }

    /** ユーザーにタグをつけるダイアログを開く */
    fun openUserTagSelectionDialog(
        user: String,
        fragmentManager: FragmentManager,
        coroutineScope: CoroutineScope
    ) = coroutineScope.launch(Dispatchers.Main) {
        repository.loadUserTags()
        val userAndTags = repository.loadUserTags(user).let {
            it.value?.tags?.map { tag -> tag.id }.orEmpty()
        }

        val dialog = UserTagSelectionDialog.createInstance(
            user,
            repository.userTags,
            userAndTags
        )

        dialog.setOnActivateTagsListener { (user, activeTags) ->
            activeTags.forEach { tag ->
                repository.tagUser(user, tag)
            }
        }

        dialog.setOnInactivateTagsListener { (user, inactiveTags) ->
            inactiveTags.forEach { tag ->
                repository.unTagUser(user, tag)
            }
        }

        dialog.setOnAddNewTagListener { f ->
            openUserTagCreationDialog(user, f.parentFragmentManager)
        }

        dialog.setOnCompleteListener {
            repository.refreshBookmarks()
        }

        dialog.showAllowingStateLoss(fragmentManager)
    }

    /** 新しいタグを作成してユーザーにつけるダイアログを開く */
    fun openUserTagCreationDialog(user: String?, fragmentManager: FragmentManager) {
        val dialog = UserTagDialogFragment.createInstance()

        dialog.setOnCompleteListener { (tagName) ->
            val context = SatenaApplication.instance
            val result = runCatching {
                repository.createUserTag(tagName)
            }
            if (result.isFailure) {
                when (val e = result.exceptionOrNull()) {
                    is AlreadyExistedException -> {
                        context.showToast(R.string.msg_user_tag_existed)
                    }

                    else -> {
                        Log.e("UserTagCreation", Log.getStackTraceString(e))
                        context.showToast(R.string.msg_user_tag_creation_failure)
                    }
                }
                return@setOnCompleteListener false
            }

            val tag = result.getOrNull()
            if (!user.isNullOrBlank() && tag != null) {
                try {
                    repository.tagUser(user, tag)
                    repository.refreshBookmarks()
                    context.showToast(
                        R.string.msg_user_tag_created_and_added_user,
                        tagName,
                        user
                    )
                }
                catch (e: TaskFailureException) {
                    Log.e("UserTagCreation", Log.getStackTraceString(e))
                    context.showToast(R.string.msg_user_tag_selection_failure)
                }
            }
            else {
                context.showToast(R.string.msg_user_tag_created, tagName)
            }

            true
        }

        dialog.showAllowingStateLoss(fragmentManager)
    }

    /** 自分がつけたスターを取り消すダイアログを開く */
    fun openDeleteStarDialog(
        entry: Entry,
        bookmark: Bookmark,
        stars: List<Star>,
        fragmentManager: FragmentManager
    ) {
        val dialog = StarDeletionDialog.createInstance(stars)

        dialog.setOnDeleteStars { selectedStars, f ->
            val activity = f.requireActivity()
            activity.lifecycleScope.launch(Dispatchers.Main) {
                val completed = selectedStars.all { star ->
                    val result = runCatching {
                        repository.deleteStar(entry, bookmark, star, updateCacheImmediately = false)
                    }
                    result.isSuccess
                }

                if (completed) {
                    activity.showToast(R.string.msg_delete_star_succeeded)
                }
                else {
                    activity.showToast(R.string.msg_delete_star_failed)
                }

                // スター表示の更新
                runCatching {
                    repository.getStarsEntry(bookmark, forceUpdate = true)
                    repository.refreshBookmarks()
                }
            }
        }

        dialog.showAllowingStateLoss(fragmentManager)
    }

    /** 自分のブクマを削除するか確認するダイアログを開く */
    private fun openConfirmBookmarkDeletionDialog(
        context: Context,
        bookmark: Bookmark,
        fragmentManager: FragmentManager,
        coroutineScope: CoroutineScope
    ) {
        AlertDialogFragment.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(R.string.msg_confirm_bookmark_deletion)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) {
                coroutineScope.launch(Dispatchers.Main) {
                    val result = runCatching {
                        repository.deleteBookmark(bookmark)
                    }

                    if (result.isSuccess) {
                        context.showToast(R.string.msg_remove_bookmark_succeeded)
                    }
                    else {
                        context.showToast(R.string.msg_remove_bookmark_failed)
                    }
                }
            }
            .create()
            .showAllowingStateLoss(fragmentManager)
    }
}
