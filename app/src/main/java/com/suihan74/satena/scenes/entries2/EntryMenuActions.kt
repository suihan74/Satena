package com.suihan74.satena.scenes.entries2;

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.models.EntryReadActionType
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.models.readEntry.ReadEntryCondition
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.BookmarksActivityContract
import com.suihan74.satena.scenes.entries2.dialog.EntryMenuDialog2
import com.suihan74.satena.scenes.entries2.dialog.ShareEntryDialog
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.createIntentWithoutThisApplication
import com.suihan74.utilities.extensions.putObjectExtra
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        activity: FragmentActivity,
        entry: Entry,
        entryAction: TapEntryAction,
        fragmentManager: FragmentManager
    ) {
        when (entryAction) {
            TapEntryAction.SHOW_COMMENTS -> showComments(activity, entry)

            TapEntryAction.SHOW_PAGE -> showPage(activity, entry)

            TapEntryAction.SHOW_PAGE_IN_BROWSER -> showPageInBrowser(activity, entry)

            TapEntryAction.SHARE -> sharePage(activity, entry)

            TapEntryAction.SHOW_MENU -> openMenuDialog(entry, fragmentManager)

            TapEntryAction.NOTHING -> {}
        }
    }

    /**
     * エントリ項目に対するメニューダイアログを開く
     */
    fun openMenuDialog(
        entry: Entry,
        fragmentManager: FragmentManager
    ) {
        val dialog = EntryMenuDialog2.createInstance(entry).apply {
            setShowCommentsListener { entry, f ->
                showComments(f.requireActivity(), entry)
            }
            setShowPageListener { entry, f->
                showPage(f.requireActivity(), entry)
            }
            setShowPageInBrowserListener { entry, f ->
                showPageInBrowser(f.requireActivity(), entry)
            }
            setSharePageListener { entry, f ->
                sharePage(f.requireActivity(), entry)
            }
            setShowEntriesListener { entry, f ->
                showEntries(f.requireActivity(), entry)
            }
            setFavoriteEntryListener { entry, f ->
                val a = f.requireActivity()
                favoriteEntry(a, entry, a.lifecycleScope)
            }
            setUnfavoriteEntryListener { entry, f ->
                val a = f.requireActivity()
                unfavoriteEntry(a, entry, a.lifecycleScope)
            }
            setIgnoreEntryListener { entry, f ->
                val a = f.requireActivity()
                openIgnoreEntryDialog(a, entry, a.supportFragmentManager, a.lifecycleScope)
            }
            setReadLaterListener { entry, f ->
                val a = f.requireActivity()
                readLaterEntry(a, entry, a.lifecycleScope)
            }
            setReadListener { entry, f ->
                val a = f.requireActivity()
                readEntry(a, entry, a.lifecycleScope)
            }
            setDeleteBookmarkListener { entry, f ->
                val a = f.requireActivity()
                deleteEntryBookmark(a, entry, a.lifecycleScope)
            }
            setDeleteReadMarkListener { entry, f ->
                val a = f.requireActivity()
                deleteReadMark(a, entry, a.lifecycleScope)
            }
        }
        dialog.showAllowingStateLoss(fragmentManager, DIALOG_ENTRY_MENU)
    }

    // ------ //

    /** ブクマ一覧画面を開く */
    fun showComments(activity: FragmentActivity, entry: Entry)

    /** 内部ブラウザでページを開く */
    fun showPage(activity: FragmentActivity, entry: Entry)

    /** 外部ブラウザでページを開く */
    fun showPageInBrowser(activity: FragmentActivity, entry: Entry)

    /** エントリを「共有」 */
    fun sharePage(activity: FragmentActivity, entry: Entry)

    /** サイトのエントリ一覧を開く */
    fun showEntries(activity: FragmentActivity, entry: Entry)

    /** サイトをお気に入りに追加する */
    fun favoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope)

    /** サイトをお気に入りから除外する */
    fun unfavoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope)

    /** 非表示設定を追加するダイアログを開く */
    fun openIgnoreEntryDialog(
        activity: FragmentActivity,
        entry: Entry,
        fragmentManager: FragmentManager,
        coroutineScope: CoroutineScope
    )

    /** あとで読む */
    fun readLaterEntry(
        activity: FragmentActivity,
        entry: Entry,
        coroutineScope: CoroutineScope
    )

    /** 「あとで読む」エントリを「読んだ」 */
    fun readEntry(
        activity: FragmentActivity,
        entry: Entry,
        coroutineScope: CoroutineScope
    )

    /** ブクマを削除する */
    fun deleteEntryBookmark(
        activity: FragmentActivity,
        entry: Entry,
        coroutineScope: CoroutineScope
    )

    /** 既読マークを削除する */
    fun deleteReadMark(
        activity: FragmentActivity,
        entry: Entry,
        coroutineScope: CoroutineScope
    )
}

// ------ //

/** 画面に依らない共通の処理の実装 */

abstract class EntryMenuActionsImplBasic : EntryMenuActions {
    private fun readEntry(entry: Entry) =SatenaApplication.instance.coroutineScope.launch {
        SatenaApplication.instance.readEntriesRepository
            .insert(entry, ReadEntryCondition.PAGE_SHOWN)
    }

    override fun showComments(activity: FragmentActivity, entry: Entry) {
        val intent = Intent(activity, BookmarksActivity::class.java).also {
            it.putObjectExtra(BookmarksActivity.EXTRA_ENTRY, entry)
        }
        activity.startActivity(intent)
    }

    override fun showPage(activity: FragmentActivity, entry: Entry) {
        readEntry(entry)
        activity.startInnerBrowser(entry)
    }

    override fun showPageInBrowser(activity: FragmentActivity, entry: Entry) {
        try {
            val intent = Intent().let {
                it.action = Intent.ACTION_VIEW
                it.data = Uri.parse(entry.url)

                it.createIntentWithoutThisApplication(activity, entry.url)
            }

            checkNotNull(intent.resolveActivity(activity.packageManager)) {
                "cannot resolve intent for browsing the web site: ${entry.url}"
            }

            readEntry(entry)
            activity.startActivity(intent)
        }
        catch (e: Throwable) {
            Log.e("sharePage", Log.getStackTraceString(e))
            activity.showToast(R.string.msg_show_page_in_browser_failed)
        }
    }

    override fun sharePage(activity: FragmentActivity, entry: Entry) {
        ShareEntryDialog.createInstance(entry)
            .show(activity.supportFragmentManager, null)
    }

    override fun openIgnoreEntryDialog(
        activity: FragmentActivity,
        entry: Entry,
        fragmentManager: FragmentManager,
        coroutineScope: CoroutineScope
    ) {
        val dialog = IgnoredEntryDialogFragment.createInstance(
            url = entry.url,
            title = entry.title
        )

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_IGNORE_SITE)
    }
}

// ------ //

/** エントリ画面用の実装 */
class EntryMenuActionsImplForEntries(
    private val repository : EntriesRepository,
    private val readEntriesRepo : ReadEntriesRepository
) : EntryMenuActionsImplBasic() {

    override fun showComments(activity: FragmentActivity, entry: Entry) {
        activity.alsoAs<EntriesActivity> { a ->
            a.bookmarksActivityLauncher.launch(
                BookmarksActivityContract.Args(entry = entry)
            )
        }
    }

    override fun showEntries(activity: FragmentActivity, entry: Entry) {
        activity.alsoAs<EntriesActivity> { a ->
            a.showSiteEntries(entry.rootUrl)
        }
    }

    override fun favoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                repository.favoriteSitesRepo.favoriteEntrySite(entry)
            }.onSuccess {
                context.showToast(R.string.msg_favorite_site_registration_succeeded)
            }.onFailure {
                Log.e("favoriteEntry", Log.getStackTraceString(it))
            }
        }
    }

    override fun unfavoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                repository.favoriteSitesRepo.unfavoriteEntrySite(entry)
            }.onSuccess {
                context.showToast(R.string.msg_favorite_site_deletion_succeeded)
            }
        }
    }

    override fun readLaterEntry(activity: FragmentActivity, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                repository.readLaterEntry(entry)
            }.onSuccess { bookmarkResult ->
                activity.alsoAs<EntriesActivity> { a -> a.updateBookmark(entry, bookmarkResult) }
                activity.showToast(R.string.msg_post_bookmark_succeeded)
            }.onFailure {
                activity.showToast(R.string.msg_post_bookmark_failed)
            }
        }
    }

    override fun readEntry(activity: FragmentActivity, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                repository.readEntry(entry)
            }.onSuccess { (action, bookmarkResult) ->
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
                        activity.startActivity(intent)
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
            }.onFailure {
                activity.showToast(R.string.msg_post_bookmark_failed)
                return@launch
            }
        }
    }

    override fun deleteEntryBookmark(
        activity: FragmentActivity,
        entry: Entry,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                repository.deleteBookmark(entry)
            }.onSuccess {
                activity.alsoAs<EntriesActivity> { a -> a.removeBookmark(entry) }
                activity.showToast(R.string.msg_remove_bookmark_succeeded)
            }.onFailure {
                activity.showToast(R.string.msg_remove_bookmark_failed)
            }
        }
    }

    override fun deleteReadMark(
        activity: FragmentActivity,
        entry: Entry,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                readEntriesRepo.delete(entry)
                activity.alsoAs<EntriesActivity> {
                    it.updateBookmark(entry, entry.bookmarkedData)
                }
            }.onFailure {
                activity.showToast(R.string.msg_remove_read_mark_failed)
            }
        }
    }
}
