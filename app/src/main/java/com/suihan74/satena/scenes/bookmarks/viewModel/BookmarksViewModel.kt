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
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.models.saveHistory
import com.suihan74.satena.scenes.bookmarks.*
import com.suihan74.satena.scenes.bookmarks.dialog.CustomTabSettingsDialog
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.post.BookmarkEditData
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.Listener
import com.suihan74.utilities.OnSuccess
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.ToastTag
import com.suihan74.utilities.extensions.getObjectExtra
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.*

class BookmarksViewModel(
    val repository: BookmarksRepository
) : ViewModel() {

    /** サインイン状態 */
    val signedIn = repository.signedIn

    /** 表示中のページのEntry */
    val entry = repository.entry

    /** 表示中のページのBookmarksEntry */
    val bookmarksEntry by lazy {
        repository.bookmarksEntry.also { bEntry ->
            bEntry.observeForever {
                val bookmarks = it?.bookmarks ?: return@observeForever
                val users =
                    bookmarks.size.let { listSize ->
                        if (it.count > listSize && listSize == 0) it.count
                        else listSize
                    }

                val comments = bookmarks.count { b -> b.comment.isNotBlank() }
                subtitle.value = buildString {
                    append(users, " user")
                    if (users != 1) append("s")

                    if (comments > 0) {
                        append(" (", comments, " comment")
                        if (comments != 1) append("s")
                        append(")")
                    }
                }
            }
        }
    }

    /** エントリにつけられたスター */
    val entryStarsEntry = repository.entryStarsEntry

    /** アクションバーに表示するサブタイトル */
    val subtitle = MutableLiveData<String>()

    /** 人気ブクマリスト */
    val popularBookmarks = repository.popularBookmarks

    /** 新着ブクマリスト */
    val recentBookmarks = repository.recentBookmarks

    /** 無言や非表示を含むすべての新着ブクマリスト */
    val allBookmarks = repository.allBookmarks

    /** ユーザータグによる抽出を行う新着ブクマリスト */
    val customBookmarks = repository.customBookmarks

    /** サインインしているユーザーのブクマ */
    val userBookmark : Bookmark?
        get() = repository.userSignedIn?.let { user ->
            bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == user }
        }

    /** ユーザーの所持カラースター数 */
    val userColorStarsCount = repository.userColorStarsCount

    /** フィルタテキスト */
    val filteringText = repository.filteringText

    /** 途中で中断されたブコメ編集内容 */
    var editData : BookmarkEditData? = null

    /** すべての操作を停止した状態で行うロード中 */
    val staticLoading = repository.staticLoading

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

    private enum class ErrorToastTag : ToastTag {
        LOAD_BOOKMARKS_FAILURE
    }

    /** ブクマリスト取得失敗時のエラーメッセージ */
    private fun showLoadingErrorMessage(context: Context, e: Throwable) {
        val msgId = when (e) {
            is CancellationException -> return
            is ForbiddenException -> R.string.msg_bookmark_comments_are_hidden
            is IllegalArgumentException -> R.string.invalid_url_error
            else -> R.string.msg_update_bookmarks_failed
        }
        Log.w("loadingBookmarksFailure", e.stackTraceToString())
        context.showToast(msgId, ErrorToastTag.LOAD_BOOKMARKS_FAILURE)
    }

    // ------ //

    fun onCreate(owner: LifecycleOwner) {
        viewModelScope.launch {
            runCatching {
                repository.signIn()
            }
        }
    }

    fun loadEntryFromIntent(
        activity: AppCompatActivity,
        intent: Intent
    ) = viewModelScope.launch(Dispatchers.Main) {
        runCatching {
            repository.loadEntryFromIntent(intent)
        }
        .onSuccess {
            entry.value?.saveHistory(activity)
        }
        .onFailure {
            showLoadingErrorMessage(activity, it)

            // 不正なURLが渡された場合は閉じる
            if (it is IllegalArgumentException) {
                activity.lifecycleScope.launch {
                    activity.finish()
                }
            }
        }
    }

    /**
     * クリックに伴う画面遷移などのイベントリスナを設定する
     */
    fun initializeListeners(activity: AppCompatActivity) {
        onLinkClicked = { url ->
            val action = TapEntryAction.fromId(repository.prefs.getInt(
                PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION
            ))
            invokeLinkAction(activity, action) {
                repository.getEntry(url)
            }
        }

        onLinkLongClicked = { url ->
            val action = TapEntryAction.fromId(repository.prefs.getInt(
                PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION
            ))
            invokeLinkAction(activity, action) {
                repository.getEntry(url)
            }
        }

        onEntryIdClicked = { eid ->
            val action = TapEntryAction.fromId(repository.prefs.getInt(
                PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION
            ))
            invokeLinkAction(activity, action) {
                repository.getEntry(eid)
            }
        }

        onEntryIdLongClicked = { eid ->
            val action = TapEntryAction.fromId(repository.prefs.getInt(
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
        val handler = EntryMenuActionsImplForBookmarks(
            repository,
            SatenaApplication.instance.favoriteSitesRepository
        )

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

    /**
     * ブクマリストタイプに合致するブクマリストのLiveDataを取得する
     */
    fun bookmarksLiveData(tab: BookmarksTabType) : LiveData<List<Bookmark>> = when(tab) {
        BookmarksTabType.POPULAR -> popularBookmarks
        BookmarksTabType.RECENT -> recentBookmarks
        BookmarksTabType.ALL -> allBookmarks
        BookmarksTabType.CUSTOM -> customBookmarks
    }

    /**
     * 最新ブクマリストを取得
     */
    suspend fun loadRecentBookmarks(context: Context, additionalLoading: Boolean = false) {
        runCatching {
            repository.loadRecentBookmarks(additionalLoading)
        }
        .onFailure {
            Log.e("loadRecentBookmarks", it.stackTraceToString())
            showLoadingErrorMessage(context, it)
        }
    }

    /**
     * 人気ブクマリストを取得
     */
    suspend fun loadPopularBookmarks(context: Context) {
        runCatching {
            repository.loadPopularBookmarks()
        }
        .onFailure {
            Log.e("loadPopularBookmarks", it.stackTraceToString())
            showLoadingErrorMessage(context, it)
        }
    }

    // ------ //

    /** 必要に応じて確認ダイアログを表示し、ブコメにスターを付ける */
    fun postStarToBookmark(
        context: Context,
        bookmark: Bookmark,
        color: StarColor,
        quote: String?,
        fragmentManager: FragmentManager,
        onSuccess: OnSuccess<Unit>? = null
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
                        runCatching {
                            postStarToBookmarkImpl(context, entry, bookmark, color, quote)
                        }
                        .onSuccess {
                            onSuccess?.invoke(Unit)
                        }
                        it.dismiss()
                    }
                }
                .dismissOnClickButton(false)
                .create()
            dialog.showAllowingStateLoss(fragmentManager)
        }
        else {
            viewModelScope.launch(Dispatchers.Main) {
                runCatching {
                    postStarToBookmarkImpl(context, entry, bookmark, color, quote)
                }
                .onSuccess {
                    onSuccess?.invoke(Unit)
                }
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
            context.showToast(R.string.msg_no_color_stars, color.name)
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
            val dialog = AlertDialogFragment.Builder()
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
    private fun openPurchaseColorStarsPage(activity: Activity) {
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

    private val bookmarkMenuActions by lazy {
        BookmarkMenuActionsImpl(repository)
    }

    /** ブクマ項目に対する操作メニューを表示 */
    suspend fun openBookmarkMenuDialog(
        bookmark: Bookmark,
        fragmentManager: FragmentManager,
    ) {
        val entry = entry.value ?: return
        val starsEntry = runCatching { repository.getStarsEntry(bookmark) }.getOrNull()
        bookmarkMenuActions.openBookmarkMenuDialog(
            entry,
            bookmark,
            starsEntry?.value,
            fragmentManager
        )
    }

    // ------ //

    /** ブクマリスト項目の「スターをつける」ボタンの準備 */
    fun setAddStarButtonBinder(
        activity: Activity,
        adapter: BookmarksAdapter,
        lifecycleOwner: LifecycleOwner,
        fragmentManager: FragmentManager
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

            viewModelScope.launch {
                val liveData = runCatching { repository.getStarsEntry(bookmark) }.getOrNull()
                val userStarred = liveData?.value?.allStars?.any { it.user == user } ?: false

                lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (userStarred) {
                        button.setImageResource(R.drawable.ic_add_star_filled)

                        button.setOnLongClickListener {
                            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                                repository.getUserStars(bookmark, user)?.let { stars ->
                                    val entry = entry.value ?: return@let
                                    bookmarkMenuActions.openDeleteStarDialog(
                                        entry,
                                        bookmark,
                                        stars,
                                        fragmentManager
                                    )
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

    /**
     * ツールバータップ時の処理
     *
     * エントリをアプリ内ブラウザで開く
     */
    fun onClickToolbar(activity: BookmarksActivity) {
        entry.value?.let { entry ->
            activity.startInnerBrowser(entry)
        }
    }

    /**
     * ツールバーロングタップ時の処理
     *
     * エントリメニューダイアログを開く
     */
    fun onLongClickToolbar(activity: BookmarksActivity) {
        entry.value?.let { entry ->
            val handler = EntryMenuActionsImplForBookmarks(
                repository,
                SatenaApplication.instance.favoriteSitesRepository
            )
            handler.openMenuDialog(activity, entry, activity.supportFragmentManager, activity.lifecycleScope)
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
