package com.suihan74.satena.scenes.bookmarks.viewModel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.suihan74.hatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment2
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks.dialog.*
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks2.AddStarPopupMenu
import com.suihan74.satena.scenes.bookmarks2.BookmarksAdapter
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntryMenuActionsImplForBookmarks
import com.suihan74.satena.scenes.post.BookmarkEditData
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.utilities.Listener
import com.suihan74.utilities.OnFinally
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.exceptions.AlreadyExistedException
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.getObjectExtra
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksViewModel(
    val repository: BookmarksRepository
) : ViewModel() {

    /** サインイン状態 */
    val signedIn
        get() = repository.signedIn

    /** 表示中のページのEntry */
    val entry
        get() = repository.entry

    /** 表示中のページのBookmarksEntry */
    val bookmarksEntry by lazy {
        repository.bookmarksEntry.also { bEntry ->
            bEntry.observeForever {
                val bookmarks = it?.bookmarks ?: return@observeForever
                val users = bookmarks.size
                val comments = bookmarks.count { b -> b.comment.isNotBlank() }
                subtitle.value = buildString {
                    append(users, " user")
                    if (users != 1) append("s")

                    append(" (", comments, " comment")
                    if (comments != 1) append("s")
                    append(")")
                }
            }
        }
    }

    /** エントリにつけられたスター */
    val entryStarsEntry
        get() = repository.entryStarsEntry

    /** アクションバーに表示するサブタイトル */
    val subtitle by lazy {
        MutableLiveData<String>()
    }

    /** 人気ブクマリスト */
    val popularBookmarks
        get() = repository.popularBookmarks

    /** 新着ブクマリスト */
    val recentBookmarks
        get() = repository.recentBookmarks

    /** 無言や非表示を含むすべての新着ブクマリスト */
    val allBookmarks
        get() = repository.allBookmarks

    /** ユーザータグによる抽出を行う新着ブクマリスト */
    val customBookmarks
        get() = repository.customBookmarks

    /** サインインしているユーザーのブクマ */
    val userBookmark : Bookmark?
        get() = repository.userSignedIn?.let { user ->
            bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == user }
        }

    /** ユーザーの所持カラースター数 */
    val userColorStarsCount
        get() = repository.userColorStarsCount

    /** フィルタテキスト */
    val filteringText
        get() = repository.filteringText

    /** 途中で中断されたブコメ編集内容 */
    var editData : BookmarkEditData? = null

    // ------ //

    /**
     * コメント中のリンクがクリックされたときの処理
     */
    var onLinkClicked : Listener<String>? = null
        private set

    /**
     * コメント中のリンクが長押しされたときの処理
     */
    var onLinkLongClicked : Listener<String>? = null
        private set

    /**
     * コメント中のエントリIDがクリックされたときの処理
     */
    var onEntryIdClicked : Listener<Long>? = null
        private set

    /**
     * コメント中のエントリIDが長押しされたときの処理
     */
    var onEntryIdLongClicked : Listener<Long>? = null
        private set

    /**
     * ブクマ中のタグがクリックされたときの処理
     */
    var onTagClicked : Listener<String>? = null
        private set

    // ------ //

    init {
        viewModelScope.launch {
            repository.signIn()
        }
    }

    /**
     * クリックに伴う画面遷移などのイベントリスナを設定する
     */
    fun initializeListeners(activity: AppCompatActivity) {
        onLinkClicked = { url ->
            val action = TapEntryAction.fromInt(repository.prefs.getInt(
                PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION
            ))
            invokeLinkAction(activity, action) {
                repository.getEntry(url)
            }
        }

        onLinkLongClicked = { url ->
            val action = TapEntryAction.fromInt(repository.prefs.getInt(
                PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION
            ))
            invokeLinkAction(activity, action) {
                repository.getEntry(url)
            }
        }

        onEntryIdClicked = { eid ->
            val action = TapEntryAction.fromInt(repository.prefs.getInt(
                PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION
            ))
            invokeLinkAction(activity, action) {
                repository.getEntry(eid)
            }
        }

        onEntryIdLongClicked = { eid ->
            val action = TapEntryAction.fromInt(repository.prefs.getInt(
                PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION
            ))
            invokeLinkAction(activity, action) {
                repository.getEntry(eid)
            }
        }

        onTagClicked = { tag ->
            val intent = Intent(activity, EntriesActivity::class.java).also {
                it.putExtra(EntriesActivity.EXTRA_SEARCH_TAG, tag)
            }
            activity.startActivity(intent)
        }
    }

    /**
     * コメント中のリンクをクリック/長押ししたときの処理
     */
    private fun invokeLinkAction(
        activity: AppCompatActivity,
        actionType: TapEntryAction,
        entryLoader: suspend ()->Entry
    ) {
        val handler = EntryMenuActionsImplForBookmarks(FavoriteSitesRepository(
            SafeSharedPreferences.create(activity),
            HatenaClient
        ))

        activity.lifecycleScope.launch(Dispatchers.Main) {
            val result = runCatching {
                entryLoader()
            }

            val entry = result.getOrElse {
                activity.showToast(R.string.msg_get_entry_failed)
                return@launch
            }

            handler.invokeEntryClickedAction(
                activity,
                entry,
                actionType,
                activity.supportFragmentManager,
                viewModelScope
            )
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
    fun postStarToBookmark(
        context: Context,
        bookmark: Bookmark,
        color: StarColor,
        quote: String?,
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
                    viewModelScope.launch {
                        postStarToBookmarkImpl(context, entry, bookmark, color, quote)
                        it.dismiss()
                    }
                }
                .dismissOnClickButton(false)
                .create()
            dialog.showAllowingStateLoss(fragmentManager)
        }
        else {
            viewModelScope.launch {
                postStarToBookmarkImpl(context, entry, bookmark, color, quote)
            }
        }
    }

    private suspend fun postStarToBookmarkImpl(
        context: Context,
        entry: Entry,
        bookmark: Bookmark,
        color: StarColor,
        quote: String?
    ) = withContext(Dispatchers.Main) {
        val starAvailable = repository.checkColorStarAvailability(color)
        if (!starAvailable) {
            context.showToast(
                R.string.msg_post_star_failed,
                bookmark.user
            )
            return@withContext
        }

        val result = runCatching {
            repository.postStar(entry, bookmark, color, quote.orEmpty())
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

    /** 必要に応じて確認ダイアログを表示し、エントリにスターを付ける */
    fun postStarToEntry(
        context: Context,
        color: StarColor,
        fragmentManager: FragmentManager
    ) {
        val entry = entry.value ?: let {
            context.showToast(R.string.msg_post_star_to_entry_failed)
            return
        }

        if (repository.useConfirmPostingStarDialog) {
            val dialog = AlertDialogFragment2.Builder()
                .setTitle(R.string.confirm_dialog_title_simple)
                .setMessage(context.getString(R.string.msg_post_star_dialog, color.name))
                .setNegativeButton(R.string.dialog_cancel) { it.dismiss() }
                .setPositiveButton(R.string.dialog_ok) {
                    viewModelScope.launch {
                        postStarToEntryImpl(context, entry, color)
                        it.dismiss()
                    }
                }
                .dismissOnClickButton(false)
                .create()
            dialog.showAllowingStateLoss(fragmentManager)
        }
        else {
            viewModelScope.launch {
                postStarToEntryImpl(context, entry, color)
            }
        }
    }

    private suspend fun postStarToEntryImpl(
        context: Context,
        entry: Entry,
        color: StarColor
    ) = withContext(Dispatchers.Main) {
        val starAvailable = repository.checkColorStarAvailability(color)
        if (!starAvailable) {
            context.showToast(R.string.msg_post_star_to_entry_failed)
            return@withContext
        }

        val result = runCatching {
            repository.postStar(entry, color)
        }

        if (result.isSuccess) {
            context.showToast(R.string.msg_post_star_to_entry_succeeded)

            // 表示を更新する
            repository.refreshBookmarks()
        }
        else {
            Log.w("postStar", Log.getStackTraceString(result.exceptionOrNull()))
            context.showToast(R.string.msg_post_star_to_entry_failed)
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

    /**
     * スター付与ポップアップを開く
     */
    fun openAddStarPopup(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        anchor: View,
        postingAction: (color: StarColor)->Unit
    ) {
        AddStarPopupMenu(activity).run {
            observeUserStars(lifecycleOwner, userColorStarsCount)

            setOnClickAddStarListener { color ->
                postingAction(color)
                dismiss()
            }

            setOnClickPurchaseStarsListener {
                openPurchaseColorStarsPage(activity)
                dismiss()
            }

            showAsDropDown(anchor)
        }
    }

    // ------ //

    private val DIALOG_BOOKMARK_MENU by lazy { "DIALOG_BOOKMARK_MENU" }

    /** ブクマ項目に対する操作メニューを表示 */
    fun openBookmarkMenuDialog(
        activity: Activity,
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

        dialog.setOnShowEntries { showEntries(activity, it) }

        dialog.setOnIgnoreUser { ignoreUser(it) }

        dialog.setOnUnignoreUser { unIgnoreUser(it) }

        dialog.setOnReportBookmark { reportBookmark(it, fragmentManager) }

        dialog.setOnSetUserTag { openUserTagSelectionDialog(it, fragmentManager) }

        dialog.setOnDeleteStar { openDeleteStarDialog(it.first, it.second, fragmentManager) }

        dialog.setOnDeleteBookmark { openConfirmBookmarkDeletionDialog(activity, it, fragmentManager) }

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

            dialog.setOnAddNewTagListener {
                openUserTagCreationDialog(user, fragmentManager)
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
        AlertDialogFragment2.Builder()
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
        adapter.setAddStarButtonBinder { button, bookmark ->
            button.setOnClickListener {
                openAddStarPopup(activity, lifecycleOwner, button) { color ->
                    postStarToBookmark(
                        activity,
                        bookmark,
                        color,
                        null,
                        fragmentManager
                    )
                }
            }

            val user = repository.userSignedIn
            if (user == null) {
                button.setVisibility(false)
            }
            else {
                button.setVisibility(true)
                TooltipCompat.setTooltipText(button, activity.getString(R.string.add_star_popup_desc))
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

    // ------ //

    /** 「カスタム」タブの表示対象を設定するダイアログを開く */
    fun openCustomTabSettingsDialog(fragmentManager: FragmentManager) {
        viewModelScope.launch(Dispatchers.Main) {
            // 確実にダイアログで全タグを表示するため
            // ユーザータグリストを読み込んでおく
            repository.loadUserTags()

            val dialog = CustomTabSettingsDialog.createInstance(repository)

            dialog.setOnCompletedListener {
                viewModelScope.launch {
                    repository.refreshBookmarks()
                }
            }

            dialog.showAllowingStateLoss(fragmentManager)
        }
    }

    // ------ //

    /** BookmarkPostActivityの結果を反映させる */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // ブクマ投稿結果をentryに反映して、次回以降の編集時に投稿内容を最初から入力した状態でダイアログを表示する
        when (requestCode) {
            BookmarkPostActivity.REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val result = data?.getObjectExtra<BookmarkResult>(
                            BookmarkPostActivity.RESULT_BOOKMARK
                        ) ?: return

                        editData = null
                        viewModelScope.launch {
                            repository.updateBookmark(result)
                        }
                    }

                    Activity.RESULT_CANCELED -> {
                        editData = data?.getObjectExtra<BookmarkEditData>(
                            BookmarkPostActivity.RESULT_EDIT_DATA
                        )
                    }
                }
            }
        }
    }
}
