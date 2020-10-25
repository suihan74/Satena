package com.suihan74.satena.scenes.entries2;

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.fragment.app.FragmentManager
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.models.EntryReadActionType
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.entries2.dialog.EntryMenuDialog2
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepositoryForEntries
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.createIntentWithoutThisApplication
import com.suihan74.utilities.extensions.putObjectExtra
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** エントリ項目に対する操作 */
interface EntryMenuActions {
    val DIALOG_ENTRY_MENU : String
        get() = "DIALOG_ENTRY_MENU"

    val DIALOG_IGNORE_SITE : String
        get() = "DIALOG_IGNORE_SITE"

    /**
     *  項目(シングル/マルチ/ロング)クリックに対して、ユーザーが設定した処理を実行する
     */
    fun invokeEntryClickedAction(
        activity: Activity,
        entry: Entry,
        entryAction: TapEntryAction,
        fragmentManager: FragmentManager,
        coroutineScope: CoroutineScope
    ) {
        when (entryAction) {
            TapEntryAction.SHOW_COMMENTS -> showComments(activity, entry)

            TapEntryAction.SHOW_PAGE -> showPage(activity, entry)

            TapEntryAction.SHOW_PAGE_IN_BROWSER -> sharePage(activity, entry)

            TapEntryAction.SHOW_MENU ->
                openMenuDialog(activity, entry, fragmentManager, coroutineScope)

            TapEntryAction.NOTHING -> {}
        }
    }

    /**
     * エントリ項目に対するメニューダイアログを開く
     */
    fun openMenuDialog(
        activity: Activity,
        entry: Entry,
        fragmentManager: FragmentManager,
        coroutineScope: CoroutineScope
    ) {
        EntryMenuDialog2.createInstance(entry).run {
            setShowCommentsListener {
                showComments(activity, entry)
            }
            setShowPageListener {
                showPage(activity, entry)
            }
            setSharePageListener {
                sharePage(activity, entry)
            }
            setShowEntriesListener {
                showEntries(activity, entry)
            }
            setFavoriteEntryListener {
                favoriteEntry(activity, entry, coroutineScope)
            }
            setUnfavoriteEntryListener {
                unfavoriteEntry(activity, entry, coroutineScope)
            }
            setIgnoreEntryListener {
                openIgnoreEntryDialog(activity, entry, fragmentManager, coroutineScope)
            }
            setReadLaterListener {
                readLaterEntry(activity, entry, coroutineScope)
            }
            setReadListener {
                readEntry(activity, entry, coroutineScope)
            }
            setDeleteBookmarkListener {
                deleteEntryBookmark(activity, entry, coroutineScope)
            }

            showAllowingStateLoss(fragmentManager, DIALOG_ENTRY_MENU)
        }
    }

    // ------ //

    /** ブクマ一覧画面を開く */
    fun showComments(activity: Activity, entry: Entry)

    /** 内部ブラウザでページを開く */
    fun showPage(activity: Activity, entry: Entry)

    /** 外部ブラウザでページを開く(一般にいう「共有」) */
    fun sharePage(activity: Activity, entry: Entry)

    /** サイトのエントリ一覧を開く */
    fun showEntries(activity: Activity, entry: Entry)

    /** サイトをお気に入りに追加する */
    fun favoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope)

    /** サイトをお気に入りから除外する */
    fun unfavoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope)

    /** 非表示設定を追加するダイアログを開く */
    fun openIgnoreEntryDialog(
        activity: Activity,
        entry: Entry,
        fragmentManager: FragmentManager,
        coroutineScope: CoroutineScope
    )

    /** あとで読む */
    fun readLaterEntry(
        activity: Activity,
        entry: Entry,
        coroutineScope: CoroutineScope
    )

    /** 「あとで読む」エントリを「読んだ」 */
    fun readEntry(
        activity: Activity,
        entry: Entry,
        coroutineScope: CoroutineScope
    )

    /** ブクマを削除する */
    fun deleteEntryBookmark(
        activity: Activity,
        entry: Entry,
        coroutineScope: CoroutineScope
    )
}

// ------ //

/** 画面に依らない共通の処理の実装 */

abstract class EntryMenuActionsImplBasic : EntryMenuActions {
    override fun showComments(activity: Activity, entry: Entry) {
        val intent = Intent(activity, BookmarksActivity::class.java).also {
            it.putObjectExtra(BookmarksActivity.EXTRA_ENTRY, entry)
        }
        activity.startActivity(intent)
    }

    override fun showPage(activity: Activity, entry: Entry) {
        activity.startInnerBrowser(entry)
    }

    override fun sharePage(activity: Activity, entry: Entry) {
        try {
            val intent = Intent().let {
                it.action = Intent.ACTION_VIEW
                it.data = Uri.parse(entry.url)

                it.createIntentWithoutThisApplication(activity)
            }

            checkNotNull(intent.resolveActivity(activity.packageManager)) {
                "cannot resolve intent for browsing the web site: ${entry.url}"
            }

            activity.startActivity(intent)
        }
        catch (e: Throwable) {
            Log.e("sharePage", Log.getStackTraceString(e))
            activity.showToast(R.string.msg_show_page_in_browser_failed)
        }
    }
}

// ------ //

/** エントリ画面用の実装 */
class EntryMenuActionsImplForEntries(
    private val repository: EntriesRepository
) : EntryMenuActionsImplBasic() {

    override fun showEntries(activity: Activity, entry: Entry) {
        activity.alsoAs<EntriesActivity> { a ->
            a.showSiteEntries(entry.rootUrl)
        }
    }

    override fun favoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                repository.favoriteEntrySite(entry)
            }
            if (result.isSuccess) {
                context.showToast(R.string.msg_favorite_site_registration_succeeded)
            }
            else {
                Log.e("favoriteEntry", Log.getStackTraceString(result.exceptionOrNull()))
            }
        }
    }

    override fun unfavoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                repository.unfavoriteEntrySite(entry)
            }
            if (result.isSuccess) {
                context.showToast(R.string.msg_favorite_site_deletion_succeeded)
            }
        }
    }

    override fun openIgnoreEntryDialog(
        activity: Activity,
        entry: Entry,
        fragmentManager: FragmentManager,
        coroutineScope: CoroutineScope
    ) {
        IgnoredEntryDialogFragment.createInstance(
                url = entry.url,
                title = entry.title,
                positiveAction = { dialog, ignoredEntry ->
                    coroutineScope.launch(Dispatchers.Main) {
        try {
            withContext(Dispatchers.IO) {
                repository.addIgnoredEntry(ignoredEntry)
            }

            activity.alsoAs<EntriesActivity> { a ->
                a.refreshLists()
            }

            activity.showToast(
                    R.string.msg_ignored_entry_dialog_succeeded,
                    ignoredEntry.query
            )

            dialog.dismiss()
        }
        catch (e: Throwable) {
            activity.showToast(R.string.msg_ignored_entry_dialog_failed)
        }
                }
        false
            }
        ).run {
            showAllowingStateLoss(fragmentManager, DIALOG_IGNORE_SITE)
        }
    }

    override fun readLaterEntry(activity: Activity, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                repository.readLaterEntry(entry)
            }

            if (result.isSuccess) {
                val bookmarkResult = result.getOrNull()!!
                activity.alsoAs<EntriesActivity> { a ->
                    a.updateBookmark(entry, bookmarkResult)
                }
                activity.showToast(R.string.msg_post_bookmark_succeeded)
            }
            else {
                activity.showToast(R.string.msg_post_bookmark_failed)
            }
        }
    }

    override fun readEntry(activity: Activity, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                repository.readEntry(entry)
            }

            if (result.isSuccess) {
                val (action, bookmarkResult) = result.getOrNull()!!
                        when (action) {
                    EntryReadActionType.REMOVE -> {
                        activity.alsoAs<EntriesActivity> { a ->
                            a.removeBookmark(entry)
                        }
                        activity.showToast(R.string.msg_remove_bookmark_succeeded)
                    }

                    EntryReadActionType.DIALOG -> {
                        // ブクマ編集ダイアログに遷移する
                        // あとで戻ってきたときに画面を更新する --> Activity#onActivityResult
                        val intent = Intent(activity, BookmarkPostActivity::class.java).also {
                            it.putObjectExtra(BookmarkPostActivity.EXTRA_ENTRY, entry)
                        }
                        activity.startActivityForResult(
                            intent,
                            BookmarkPostActivity.REQUEST_CODE
                        )
                    }

                    else -> {
                        if (bookmarkResult != null) {
                            activity.alsoAs<EntriesActivity> { a ->
                                a.updateBookmark(entry, bookmarkResult)
                            }
                        }
                        activity.showToast(R.string.msg_post_bookmark_succeeded)
                    }
                }
            }
            else {
                activity.showToast(R.string.msg_post_bookmark_failed)
            }
        }
    }

    override fun deleteEntryBookmark(
        activity: Activity,
        entry: Entry,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch(Dispatchers.Main) {
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

// ------ //

/** ブクマ画面用の実装 */
class EntryMenuActionsImplForBookmarks(
    private val favoriteSitesRepo: FavoriteSitesRepositoryForEntries
) : EntryMenuActionsImplBasic() {

    override fun showEntries(activity: Activity, entry: Entry) {
        activity.alsoAs<EntriesActivity> { a ->
            a.showSiteEntries(entry.rootUrl)
        }
    }

    override fun favoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                favoriteSitesRepo.favoriteEntrySite(entry)
            }
            if (result.isSuccess) {
                context.showToast(R.string.msg_favorite_site_registration_succeeded)
            }
        }
    }

    override fun unfavoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                favoriteSitesRepo.unfavoriteEntrySite(entry)
            }
            if (result.isSuccess) {
                context.showToast(R.string.msg_favorite_site_deletion_succeeded)
            }
        }
    }

    override fun openIgnoreEntryDialog(
        activity: Activity,
        entry: Entry,
        fragmentManager: FragmentManager,
        coroutineScope: CoroutineScope
    ) {
        IgnoredEntryDialogFragment.createInstance(
            url = entry.url,
            title = entry.title,
            positiveAction = { dialog, ignoredEntry ->
                coroutineScope.launch(Dispatchers.Main) {
                    try {
                        withContext(Dispatchers.IO) {
// TODO:
//                            repository.addIgnoredEntry(ignoredEntry)
                        }

                        activity.alsoAs<EntriesActivity> { a ->
                            a.refreshLists()
                        }

                        activity.showToast(
                            R.string.msg_ignored_entry_dialog_succeeded,
                            ignoredEntry.query
                        )

                        dialog.dismiss()
                    }
                    catch (e: Throwable) {
                        activity.showToast(R.string.msg_ignored_entry_dialog_failed)
                    }
                }
                false
            }
        ).run {
            showAllowingStateLoss(fragmentManager, DIALOG_IGNORE_SITE)
        }
    }

    override fun readLaterEntry(activity: Activity, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
//TODO:
//                repository.readLaterEntry(entry)
            }
/*
            if (result.isSuccess) {
                val bookmarkResult = result.getOrNull()!!
                activity.alsoAs<EntriesActivity> { a ->
                    a.updateBookmark(entry, bookmarkResult)
                }
                activity.showToast(R.string.msg_post_bookmark_succeeded)
            }
            else {
                activity.showToast(R.string.msg_post_bookmark_failed)
            }
*/
        }
    }

    override fun readEntry(activity: Activity, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                // TODO:
//                repository.readEntry(entry)
            }
/*
            if (result.isSuccess) {
                val (action, bookmarkResult) = result.getOrNull()!!
                when (action) {
                    EntryReadActionType.REMOVE -> {
                        activity.alsoAs<EntriesActivity> { a ->
                            a.removeBookmark(entry)
                        }
                        activity.showToast(R.string.msg_remove_bookmark_succeeded)
                    }

                    EntryReadActionType.DIALOG -> {
                        // ブクマ編集ダイアログに遷移する
                        // あとで戻ってきたときに画面を更新する --> Activity#onActivityResult
                        val intent = Intent(activity, BookmarkPostActivity::class.java).also {
                            it.putObjectExtra(BookmarkPostActivity.EXTRA_ENTRY, entry)
                        }
                        activity.startActivityForResult(
                            intent,
                            BookmarkPostActivity.REQUEST_CODE
                        )
                    }

                    else -> {
                        if (bookmarkResult != null) {
                            activity.alsoAs<EntriesActivity> { a ->
                                a.updateBookmark(entry, bookmarkResult)
                            }
                        }
                        activity.showToast(R.string.msg_post_bookmark_succeeded)
                    }
                }
            }
            else {
                activity.showToast(R.string.msg_post_bookmark_failed)
            }
 */
        }
    }

    override fun deleteEntryBookmark(
        activity: Activity,
        entry: Entry,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                // TODO:
//                repository.deleteBookmark(entry)
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
