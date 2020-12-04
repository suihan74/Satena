package com.suihan74.satena.scenes.browser.bookmarks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.Star
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.scenes.bookmarks2.AddStarPopupMenu
import com.suihan74.satena.scenes.bookmarks2.BookmarksAdapter
import com.suihan74.satena.scenes.bookmarks2.dialog.BookmarkMenuDialog
import com.suihan74.satena.scenes.bookmarks2.dialog.ReportDialog
import com.suihan74.satena.scenes.bookmarks2.dialog.StarDeletionDialog
import com.suihan74.satena.scenes.bookmarks2.dialog.UserTagSelectionDialog
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.OnFinally
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.exceptions.AlreadyExistedException
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.CoroutineScope
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
    fun reloadBookmarks(
        onFinally: OnFinally? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        runCatching {
            repository.loadRecentBookmarks(
                additionalLoading = false
            )
        }
        onFinally?.invoke()
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
            val dialog = AlertDialogFragment.Builder()
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

            // 表示を更新する
            repository.refreshBookmarks()
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
        bookmark: Bookmark,
        fragmentManager: FragmentManager
    ) = viewModelScope.launch(Dispatchers.Main) {

        val ignored = repository.checkIgnored(bookmark)

        val starsEntry = repository.getStarsEntry(bookmark)?.value

        val dialog = BookmarkMenuDialog.createInstance(
            bookmark,
            starsEntry,
            ignored,
            repository.userSignedIn
        )

        dialog.setOnShowEntries { user, f -> showEntries(f.requireActivity(), user) }

        dialog.setOnIgnoreUser { user, _ -> ignoreUser(user) }

        dialog.setOnUnignoreUser { user, _ -> unIgnoreUser(user) }

        dialog.setOnReportBookmark { b, f -> reportBookmark(b, f.parentFragmentManager) }

        dialog.setOnSetUserTag { user, f -> openUserTagSelectionDialog(user, f.parentFragmentManager) }

        dialog.setOnDeleteStar { (b, stars), f -> openDeleteStarDialog(b, stars, f.parentFragmentManager) }

        dialog.setOnDeleteBookmark { b, f ->
            openConfirmBookmarkDeletionDialog(f.requireActivity(), b, f.parentFragmentManager)
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

    /** ユーザーを非表示にする */
    private fun ignoreUser(
        user: String
    ) = viewModelScope.launch(Dispatchers.Main) {
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
    private fun unIgnoreUser(
        user: String
    ) = viewModelScope.launch(Dispatchers.Main) {
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
        bookmark: Bookmark,
        fragmentManager: FragmentManager
    ) {
        val dialog = ReportDialog.createInstance(
            user = bookmark.user,
            userIconUrl = bookmark.userIconUrl,
            comment = bookmark.commentRaw
        )

        dialog.setOnReportBookmark { model ->
            val entry = entry.value ?: return@setOnReportBookmark false

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
        fragmentManager: FragmentManager
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            repository.loadUserTags()
            val userAndTags = repository.loadUserTags(user)?.let {
                it.tags.map { tag -> tag.id }
            } ?: emptyList()

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
        bookmark: Bookmark,
        stars: List<Star>,
        fragmentManager: FragmentManager
    ) {
        val entry = entry.value ?: return
        val dialog = StarDeletionDialog.createInstance(stars)

        dialog.setOnDeleteStars { selectedStars ->
            viewModelScope.launch(Dispatchers.Main) {
                val completed = selectedStars.all { star ->
                    val result = runCatching {
                        repository.deleteStar(entry, bookmark, star, updateCacheImmediately = false)
                    }
                    result.isSuccess
                }

                val context = SatenaApplication.instance
                if (completed) {
                    context.showToast(R.string.msg_delete_star_succeeded)
                }
                else {
                    context.showToast(R.string.msg_delete_star_failed)
                }

                // スター表示の更新
                repository.getStarsEntry(bookmark, forceUpdate = true)
                repository.refreshBookmarks()
            }
        }

        dialog.showAllowingStateLoss(fragmentManager)
    }

    /** 自分のブクマを削除するか確認するダイアログを開く */
    private fun openConfirmBookmarkDeletionDialog(
        context: Context,
        bookmark: Bookmark,
        fragmentManager: FragmentManager
    ) {
        AlertDialogFragment.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(R.string.msg_confirm_bookmark_deletion)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) {
                viewModelScope.launch {
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

    // ------ //

    /** ブクマリスト項目の「スターをつける」ボタンの準備 */
    fun setAddStarButtonBinder(
        activity: Activity,
        adapter: BookmarksAdapter,
        lifecycleOwner: LifecycleOwner,
        fragmentManager: FragmentManager,
        coroutineScope: CoroutineScope = viewModelScope
    ) {
        adapter.setAddStarButtonBinder adapter@ { button, bookmark ->
            val useAddStarPopupMenu = repository.useAddStarPopupMenu
            val user = repository.userSignedIn
            if (user == null || !useAddStarPopupMenu) {
                // ボタンを使用しない
                button.setVisibility(false)
                return@adapter
            }
            else {
                button.setVisibility(true)
                TooltipCompat.setTooltipText(button, activity.getString(R.string.add_star_popup_desc))
            }

            button.setOnClickListener {
                val popup = AddStarPopupMenu(activity).also { popup ->
                    popup.observeUserStars(
                        lifecycleOwner,
                        userColorStarsCount
                    )
                    popup.setOnClickAddStarListener { color ->
                        postStar(
                            activity,
                            bookmark,
                            color,
                            fragmentManager
                        )
                        popup.dismiss()
                    }
                    popup.setOnClickPurchaseStarsListener {
                        openPurchaseColorStarsPage(activity)
                        popup.dismiss()
                    }
                }
                popup.showAsDropDown(button)
            }

            coroutineScope.launch(Dispatchers.Main) {
                val liveData = repository.getStarsEntry(bookmark)
                val userStarred = liveData?.value?.allStars?.any { it.user == user } ?: false
                if (user != null && userStarred) {
                    button.setImageResource(R.drawable.ic_add_star_filled)

                    button.setOnLongClickListener {
                        coroutineScope.launch(Dispatchers.Main) {
                            repository.getUserStars(bookmark, user)?.let { stars ->
                                openDeleteStarDialog(bookmark, stars, fragmentManager)
                            }
                        }
                        true
                    }
                }
                else {
                    button.setImageResource(R.drawable.ic_add_star)
                    button.setOnLongClickListener(null)
                }
            }
        }
    }
}
