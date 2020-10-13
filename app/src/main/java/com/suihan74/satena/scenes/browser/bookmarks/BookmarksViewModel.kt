package com.suihan74.satena.scenes.browser.bookmarks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment2
import com.suihan74.satena.scenes.bookmarks2.dialog.BookmarkMenuDialog
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.OnFinally
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarksViewModel(
    val repository: BookmarksRepository
) : ViewModel() {

    /** サインイン状態 */
    val signedIn by lazy {
        repository.signedIn
    }

    /** 表示中のページのEntry */
    val entry by lazy {
        repository.entry
    }

    /** 表示中のページのBookmarksEntry */
    val bookmarksEntry by lazy {
        repository.bookmarksEntry
    }

    /** 表示するブクマリスト */
    val bookmarks by lazy {
        repository.recentBookmarks
    }

    /** ユーザーの所持カラースター数 */
    val userColorStarsCount by lazy {
        repository.userColorStarsCount
    }

    // ------ //

    init {
        viewModelScope.launch {
            repository.signIn()
        }
    }

    // ------ //

    /** 最新ブクマリストを再取得 */
    fun reloadBookmarks(onFinally: OnFinally? = null) = viewModelScope.launch {
        viewModelScope.launch(Dispatchers.Main) {
            runCatching {
                repository.loadRecentBookmarks(
                    additionalLoading = false
                )
            }
            onFinally?.invoke()
        }
    }

    // ------ //

    /** 必要に応じて確認ダイアログを表示し、ブコメにスターを付ける */
    fun postStar(
        context: Context,
        bookmark: Bookmark,
        color: StarColor,
        fragmentManager: FragmentManager
    ) {
        val entry = entry.value ?: let {
            context.showToast(
                R.string.msg_post_star_failed,
                bookmark.user
            )
            return
        }

        if (repository.useConfirmPostingStarDialog) {
            val dialog = AlertDialogFragment2.Builder()
                .setTitle(R.string.confirm_dialog_title_simple)
                .setMessage(context.getString(R.string.msg_post_star_dialog, color.name))
                .setNegativeButton(R.string.dialog_cancel) { it.dismiss() }
                .setPositiveButton(R.string.dialog_ok) {
                    viewModelScope.launch(Dispatchers.Main) {
                        postStarImpl(context, entry, bookmark, color)
                        it.dismiss()
                    }
                }
                .dismissOnClickButton(false)
                .create()
            dialog.showAllowingStateLoss(fragmentManager)
        }
        else {
            viewModelScope.launch(Dispatchers.Main) {
                postStarImpl(context, entry, bookmark, color)
            }
        }
    }

    private suspend fun postStarImpl(
        context: Context,
        entry: Entry,
        bookmark: Bookmark,
        color: StarColor
    ) {
        val starAvailable = repository.checkColorStarAvailability(color)
        if (!starAvailable) {
            context.showToast(
                R.string.msg_post_star_failed,
                bookmark.user
            )
            return
        }

        val result = runCatching {
            repository.postStar(entry, bookmark, color)
        }

        if (result.isSuccess) {
            context.showToast(
                R.string.msg_post_star_succeeded,
                bookmark.user
            )
        }
        else {
            Log.w("postStar", Log.getStackTraceString(result.exceptionOrNull()))
            context.showToast(
                R.string.msg_post_star_failed,
                bookmark.user
            )
        }
    }

    /** カラースター購入ページを開く */
    fun openPurchaseColorStarsPage(activity: Activity) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(repository.purchaseColorStarsPageUrl)
        )
        activity.startActivity(intent)
    }

    // ------ //

    private val DIALOG_BOOKMARK_MENU by lazy { "DIALOG_BOOKMARK_MENU" }

    /** ブクマ項目に対する操作メニューを表示 */
    fun openBookmarkMenuDialog(
        activity: Activity,
        bookmark: Bookmark,
        fragmentManager: FragmentManager
    ) {
        val ignored = repository.checkIgnored(bookmark)
        val starsEntry = null // TODO

        BookmarkMenuDialog.createInstance(
            bookmark,
            starsEntry,
            ignored,
            repository.userSignedIn
        ).also { dialog ->
            dialog.setOnShowEntries { showEntries(activity, it) }
            dialog.setOnIgnoreUser { ignoreUser(it) }
            dialog.setOnUnignoreUser { unIgnoreUser(it) }
            dialog.setOnReportBookmark { reportBookmark(it) }
            dialog.setOnSetUserTag { /* TODO */ }
            dialog.setOnDeleteStar { /* TODO */ }

            dialog.showAllowingStateLoss(fragmentManager, DIALOG_BOOKMARK_MENU)
        }
    }

    /** ユーザーがブクマ済みのエントリ一覧画面を開く */
    private fun showEntries(activity: Activity, user: String) {
        val intent = Intent(activity, EntriesActivity::class.java).apply {
            putExtra(EntriesActivity.EXTRA_USER, user)
        }
        activity.startActivity(intent)
    }

    /** ユーザーを非表示にする */
    private fun ignoreUser(user: String) {
        viewModelScope.launch(Dispatchers.Main) {
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
    }

    /** ユーザーの非表示を解除する */
    private fun unIgnoreUser(user: String) {
        viewModelScope.launch(Dispatchers.Main) {
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
    }

    /** ブクマを通報する */
    private fun reportBookmark(bookmark: Bookmark) {
        // TODO
    }
}
