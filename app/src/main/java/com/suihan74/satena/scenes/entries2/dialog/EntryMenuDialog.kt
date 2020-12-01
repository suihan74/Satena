package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.NotFoundException
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.models.*
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.*
import com.suihan74.utilities.exceptions.AlreadyExistedException
import com.suihan74.utilities.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: リスナの扱い方を刷新する

class EntryMenuDialogListeners {
    /** ミュート完了時の処理 */
    var onIgnoredEntry : OnSuccess<IgnoredEntry>? = null

    /** ブクマ削除完了時の処理 */
    var onDeletedBookmark : OnSuccess<Entry>? = null

    /** ブクマ登録完了時の処理 */
    var onPostedBookmark : ((Entry, BookmarkResult)->Unit)? = null
}

/** エントリメニューダイアログ */
class EntryMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(entry: Entry) = EntryMenuDialog().withArguments {
            putObject(ARG_ENTRY, entry)
        }

        fun createInstance(url: String) = EntryMenuDialog().withArguments {
            putString(ARG_ENTRY_URL, url)
        }

        /** タップ/ロングタップ時の挙動を処理する */
        fun act(
            context: Context,
            entry: Entry,
            actionEnum: TapEntryAction,
            fragmentManager: FragmentManager,
            tag: String? = null
        ) {
            val instance = createInstance(entry)
            when (actionEnum) {
                TapEntryAction.SHOW_MENU ->
                    instance.showAllowingStateLoss(fragmentManager, tag)

                else ->
                    act(
                        instance,
                        context,
                        entry,
                        actionEnum,
                        fragmentManager,
                        tag
                    )
            }
        }

        /** タップ/ロングタップ時の挙動を処理する */
        fun act(
            context: Context,
            url: String,
            actionEnum: TapEntryAction,
            fragmentManager: FragmentManager,
            tag: String? = null
        ) {
            val instance = createInstance(url)
            when (actionEnum) {
                TapEntryAction.SHOW_MENU ->
                    instance.showAllowingStateLoss(fragmentManager, tag)

                else ->
                    act(
                        instance,
                        context,
                        url,
                        actionEnum,
                        fragmentManager,
                        tag
                    )
            }
        }

        /** タップ/ロングタップ時の挙動を処理する */
        fun act(
            context: Context,
            entry: Entry,
            actionEnum: TapEntryAction,
            listeners: EntryMenuDialogListeners,
            fragmentManager: FragmentManager,
            tag: String? = null
        ) {
            val instance = createInstance(entry).also {
                it.listeners = listeners
            }
            when (actionEnum) {
                TapEntryAction.SHOW_MENU ->
                    instance.showAllowingStateLoss(fragmentManager, tag)

                else ->
                    act(
                        instance,
                        context,
                        entry,
                        actionEnum,
                        fragmentManager,
                        tag
                    )
            }
        }

        /** タップ/ロングタップ時の挙動を処理する(メニュー表示以外の挙動) */
        private fun act(
            instance: EntryMenuDialog,
            context: Context,
            entry: Entry,
            actionEnum: TapEntryAction,
            fragmentManager: FragmentManager,
            tag: String? = null
        ) {
            val menuItem = MenuItem.values().firstOrNull { it.titleId == actionEnum.titleId } ?: return
            val listeners = instance.listeners

            fragmentManager.beginTransaction()
                .add(instance, tag)
                .runOnCommit {
                    val args = MenuItemArguments(
                        context = context,
                        entry = entry,
                        listeners = listeners
                    )
                    instance.action(menuItem, args)

                    fragmentManager.beginTransaction()
                        .remove(instance)
                        .commitAllowingStateLoss()
                }.commitAllowingStateLoss()
        }

        /** タップ/ロングタップ時の挙動を処理する(メニュー表示以外の挙動) */
        private fun act(
            instance: EntryMenuDialog,
            context: Context,
            url: String,
            actionEnum: TapEntryAction,
            fragmentManager: FragmentManager,
            tag: String? = null
        ) {
            val menuItem = MenuItem.values().firstOrNull { it.titleId == actionEnum.titleId } ?: return

            fragmentManager.beginTransaction()
                .add(instance, tag)
                .runOnCommit {
                    val args = MenuItemArguments(
                        context = context,
                        url = url
                    )
                    instance.action(menuItem, args)

                    fragmentManager.beginTransaction()
                        .remove(instance)
                        .commitAllowingStateLoss()
                }.commitAllowingStateLoss()
        }

        /** (はてなから取得できた完全な)エントリ */
        private const val ARG_ENTRY = "ARG_ENTRY"

        /** エントリが得られない場合のURL */
        private const val ARG_ENTRY_URL = "ARG_ENTRY_URL"

        /** エントリ非表示設定ダイアログ */
        private const val DIALOG_IGNORE_SITE = "DIALOG_IGNORE_SITE"
    }

    /** 永続化するデータ */
    private val viewModel by lazyProvideViewModel {
        DialogViewModel().apply {
            listeners = listeners ?: this@EntryMenuDialog.listeners
        }
    }

    /** 追加のイベントリスナセット(作成時にvmに渡すためだけに使用する) */
    private var listeners : EntryMenuDialogListeners? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val activity = requireActivity()
        val arguments = requireArguments()

        viewModel.activity = activity
        viewModel.fragmentManager = parentFragmentManager

        viewModel.repository =
            if (activity is EntriesActivity) activity.viewModel.repository
            else EntriesRepository(
                context = context,
                client = HatenaClient,
                accountLoader = AccountLoader(
                    context,
                    HatenaClient,
                    MastodonClientHolder
                ),
                ignoredEntryDao = SatenaApplication.instance.ignoredEntryDao
            )

        val url = arguments.getString(ARG_ENTRY_URL)
        val entry = arguments.getObject<Entry>(ARG_ENTRY) ?: Entry(
            id = 0,
            title = url!!,
            description = "",
            count = 0,
            url = url,
            rootUrl = HatenaClient.getTemporaryRootUrl(url),
            faviconUrl = HatenaClient.getFaviconUrl(url),
            imageUrl = ""
        )
        // urlが渡された場合、表示用にオフラインで一時的な内容を作成する

        // カスタムタイトルを生成
        val titleViewBinding = DataBindingUtil.inflate<DialogTitleEntry2Binding>(
            localLayoutInflater(),
            R.layout.dialog_title_entry2,
            null,
            false
        ).also {
            it.title = entry.title
            it.url = entry.url
            it.rootUrl = entry.rootUrl
            it.faviconUrl = entry.faviconUrl
        }

        // メニューに表示する項目リストを作成する
        val activeItems = MenuItem.values().filter { it.predicate?.invoke(entry, viewModel) != false }
        val activeItemLabels = activeItems.map { getString(it.titleId) }.toTypedArray()

        return createBuilder()
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(activeItemLabels) { _, which ->
                val args = MenuItemArguments(
                    context = activity,
                    entry = entry,
                    url = url,
                    listeners = viewModel.listeners
                )
                action(activeItems[which], args)
            }
            .create()
    }

    private fun action(item: MenuItem, args: MenuItemArguments) = GlobalScope.launch(Dispatchers.Main) {
        // ダイアログのスコープを使用すると、ダイアログ表示を伴わないアクションが実行されないので注意
        viewModel.action(item, args)
    }

    // ------ //

    /** メニュー処理用の引数セット */
    data class MenuItemArguments(
        val context: Context,
        val entry: Entry? = null,
        val url: String? = null,
        val listeners: EntryMenuDialogListeners? = null
    )

    /** メニュー項目 */
    enum class MenuItem (
        val id: Int,
        @StringRes val titleId: Int,
        val predicate: ((Entry, DialogViewModel)->Boolean)? = null
    ) {
        SHOW_COMMENTS(0,
            TapEntryAction.SHOW_COMMENTS.titleId
        ),

        SHOW_PAGE(1,
            TapEntryAction.SHOW_PAGE.titleId
        ),

        SHARE(2,
            TapEntryAction.SHOW_PAGE_IN_BROWSER.titleId
        ),

        SHOW_ENTRIES(3,
            R.string.entry_action_show_entries
        ),

        FAVORITE(8,
            R.string.entry_action_favorite,
            { entry, vm ->
                val context = vm.activity
                val prefs = SafeSharedPreferences.create<FavoriteSitesKey>(context)
                val sites = prefs.get<List<FavoriteSite>>(FavoriteSitesKey.SITES)
                sites.all { it.url != entry.rootUrl }
            }
        ),

        UNFAVORITE(9,
            R.string.entry_action_unfavorite,
            { entry, vm ->
                val context = vm.activity
                val prefs = SafeSharedPreferences.create<FavoriteSitesKey>(context)
                val sites = prefs.get<List<FavoriteSite>>(FavoriteSitesKey.SITES)
                sites.any { it.url == entry.rootUrl }
            }
        ),

        IGNORE_SITE(4,
            R.string.entry_action_ignore
        ),

        READ_LATER(5,
            R.string.entry_action_read_later,
            { entry, _ -> HatenaClient.signedIn() && entry.bookmarkedData == null }
        ),

        READ(6,
            R.string.entry_action_read,
            { entry, _ -> HatenaClient.signedIn() && entry.bookmarkedData?.tags?.contains("あとで読む") == true }
        ),

        REMOVE_BOOKMARK(7,
            R.string.bookmark_delete,
            { entry, _ -> entry.bookmarkedData != null }
        ),
    }

    // ------ //

    class DialogViewModel : androidx.lifecycle.ViewModel() {
        var activity: FragmentActivity? = null

        var fragmentManager: FragmentManager? = null

        var listeners : EntryMenuDialogListeners? = null

        var repository: EntriesRepository? = null

        /** メニュー項目ごとの処理 */
        suspend fun action(item: MenuItem, args: MenuItemArguments) = when (item) {
            MenuItem.SHOW_COMMENTS ->
                showBookmarks(args.context, args.entry, args.url)

            MenuItem.SHOW_PAGE ->
                showPage(args.context, args.entry, args.url)

            MenuItem.SHARE ->
                showPageInBrowser(args.context, args.entry, args.url)

            MenuItem.FAVORITE ->
                favorite(args)

            MenuItem.UNFAVORITE ->
                unfavorite(args)

            MenuItem.SHOW_ENTRIES ->
                showEntries(args.context, args.entry, args.url)

            MenuItem.IGNORE_SITE ->
                ignoreSite(args.context, args.entry, args.url, args.listeners?.onIgnoredEntry)

            MenuItem.READ_LATER ->
                readLater(args)

            MenuItem.READ ->
                readEntry(args)

            MenuItem.REMOVE_BOOKMARK ->
                removeBookmark(args.context, args.entry, args.url, args.listeners?.onDeletedBookmark)

            else -> throw NotImplementedError()
        }

        /** ブックマーク画面に遷移 */
        private fun showBookmarks(context: Context, entry: Entry?, url: String?) {
            val intent = Intent(context, BookmarksActivity::class.java).apply {
                addFlags(FLAG_ACTIVITY_NEW_TASK)
                if (entry != null) putObjectExtra(BookmarksActivity.EXTRA_ENTRY, entry)
                if (url != null) putExtra(BookmarksActivity.EXTRA_ENTRY_URL, url)
            }
            context.startActivity(intent)
        }

        /** ページを内部ブラウザで開く */
        private fun showPage(context: Context, entry: Entry?, url: String?) {
            try {
                if (entry != null) context.startInnerBrowser(entry)
                if (url != null) context.startInnerBrowser(url)
            }
            catch (e: Throwable) {
                Log.e("innerBrowser", Log.getStackTraceString(e))
                context.showToast(R.string.msg_show_page_failed)
                showPageInBrowser(context, entry, url)
            }
        }

        /** ページを外部ブラウザで開く */
        private fun showPageInBrowser(context: Context, entry: Entry?, url: String?) {
            try {
                val extraUrl = entry?.url ?: url!!
                val intent = Intent().let {
                    it.action = Intent.ACTION_VIEW
                    it.data = Uri.parse(extraUrl)
                    it.addFlags(FLAG_ACTIVITY_NEW_TASK)

                    // "Satena以外で"開く
                    it.createIntentWithoutThisApplication(context)
                }

                checkNotNull(intent.resolveActivity(context.packageManager)) {
                    "cannot resolve intent for browsing the website: $extraUrl"
                }
                context.startActivity(intent)
            }
            catch (e: Throwable) {
                Log.e("browser", Log.getStackTraceString(e))
                context.showToast(R.string.msg_show_page_in_browser_failed)
            }
        }

        /** サイトのエントリリストを開く */
        private fun showEntries(context: Context, entry: Entry?, url: String?) {
            val siteUrl = entry?.rootUrl ?: url!!
            val activity = this.activity ?: throw RuntimeException("EntryMenuDialog is called without an activity")

            when (activity is EntriesActivity) {
                false -> {
                    // EntriesActivity以外から呼ばれた場合、Activityを遷移する
                    val intent = Intent(context, EntriesActivity::class.java).apply {
                        putExtra(EntriesActivity.EXTRA_SITE_URL, siteUrl)
                    }
                    activity.startActivity(intent)
                }

                true -> {
                    activity.showSiteEntries(siteUrl)
                }
            }
        }

        /** サイトを非表示に設定する */
        private fun ignoreSite(context: Context, entry: Entry?, url: String?, onCompleted: OnSuccess<IgnoredEntry>?) {
            val fragmentManager = fragmentManager ?: return
            val siteUrl = entry?.url ?: url ?: return
            val dialog = IgnoredEntryDialogFragment.createInstance(
                url = siteUrl,
                title = entry?.title ?: "",
                positiveAction = { dialog, ignoredEntry ->
                    activity?.lifecycleScope?.launch(Dispatchers.Main) {
                        try {
                            withContext(Dispatchers.IO) {
                                val dao = SatenaApplication.instance.ignoredEntryDao
                                dao.insert(ignoredEntry)
                            }

                            context.showToast(R.string.msg_ignored_entry_dialog_succeeded, ignoredEntry.query)
                            onCompleted?.invoke(ignoredEntry)

                            dialog.dismiss()
                        }
                        catch (e: Throwable) {
                            context.showToast(R.string.msg_ignored_entry_dialog_failed)
                        }
                    }
                    false
                }
            )
            dialog.showAllowingStateLoss(fragmentManager, DIALOG_IGNORE_SITE)
        }

        /** ブクマを削除する */
        // TODO: BookmarksRepositoryで行うようにする
        private suspend fun removeBookmark(context: Context, entry: Entry?, url: String?, onCompleted: OnSuccess<Entry>?) {
            try {
                val target = entry?.url ?: url ?: throw RuntimeException("failed to remove a bookmark")
                // TODO: 「あとで読む」が削除できないっぽい
                HatenaClient.deleteBookmarkAsync(target).await()
                context.showToast(R.string.msg_remove_bookmark_succeeded)
                if (entry != null) {
                    onCompleted?.invoke(entry)
                }
            }
            catch (e: Throwable) {
                context.showToast(R.string.msg_remove_bookmark_failed)
                Log.e("EntryMenuDialog", Log.getStackTraceString(e))
            }
        }

        /** あとで読む */
        // TODO: BookmarksRepositoryで行うようにする
        private suspend fun readLater(args: MenuItemArguments) {
            val context = args.context
            val entry = args.entry
            try {
                val target = entry?.url ?: args.url ?: throw RuntimeException("failed to post a bookmark")
                val bookmarkResult = HatenaClient.postBookmarkAsync(target, readLater = true).await()
                context.showToast(R.string.msg_post_bookmark_succeeded)
                if (entry != null) {
                    args.listeners?.onPostedBookmark?.invoke(entry, bookmarkResult)
                }
            }
            catch (e: Throwable) {
                context.showToast(R.string.msg_post_bookmark_failed)
                Log.e("EntryMenuDialog", Log.getStackTraceString(e))
            }
        }

        /** 「あとで読む」を解除してブクマする */
        // TODO: BookmarksRepositoryで行うようにする
        private suspend fun readEntry(args: MenuItemArguments) {
            val context = args.context
            try {
                val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
                val action = EntryReadActionType.fromOrdinal(prefs.getInt(PreferenceKey.ENTRY_READ_ACTION_TYPE))

                val entry = args.entry!!
                val bookmarkResult = when(action) {
                    EntryReadActionType.SILENT_BOOKMARK ->
                        HatenaClient.postBookmarkAsync(entry.url, readLater = false).await()

                    EntryReadActionType.READ_TAG ->
                        HatenaClient.postBookmarkAsync(entry.url, comment = "[読んだ]", readLater = false).await()

                    EntryReadActionType.BOILERPLATE -> {
                        val boilerplate = prefs.getString(PreferenceKey.ENTRY_READ_ACTION_BOILERPLATE) ?: ""
                        HatenaClient.postBookmarkAsync(entry.url, comment = boilerplate).await()
                    }

                    EntryReadActionType.DIALOG -> {
                        val intent = Intent(activity, BookmarkPostActivity::class.java).apply {
                            putObjectExtra(BookmarkPostActivity.EXTRA_ENTRY, entry)
                        }
                        activity?.startActivityForResult(intent, BookmarkPostActivity.REQUEST_CODE)
                        return
                        // TODO: 結果の受け取り方をなんとかする
                    }

                    EntryReadActionType.REMOVE -> {
                        removeBookmark(context, entry, url = null, onCompleted = args.listeners?.onDeletedBookmark)
                        return
                    }
                }

                context.showToast(R.string.msg_post_bookmark_succeeded)
                args.listeners?.onPostedBookmark?.invoke(entry, bookmarkResult)
            }
            catch (e:Throwable) {
                context.showToast(R.string.msg_post_bookmark_failed)
                Log.e("EntryMenuDialog", Log.getStackTraceString(e))
            }
        }

        /** お気に入りに追加 */
        private suspend fun favorite(args: MenuItemArguments) {
            val entry = args.entry ?: return
            val context = args.context

            try {
                repository?.favoriteEntrySite(entry)
                context.showToast("お気に入りに追加しました")
            }
            catch (e: NotFoundException) {
                context.showToast("サイトが見つかりませんでした")
            }
            catch (e: AlreadyExistedException) {
                context.showToast("既に設定が存在します")
            }
            catch (e: Throwable) {
                context.showToast("お気に入り登録に失敗しました")
            }
        }

        /** お気に入りから削除する */
        private fun unfavorite(args: MenuItemArguments) {
            val entry = args.entry ?: return
            val context = args.context

            try {
                repository?.unfavoriteEntrySite(entry)
                context.showToast("お気に入りから除外しました")
            }
            catch (e: Throwable) {
                context.showToast("設定が存在しません")
            }
        }
    }
}
