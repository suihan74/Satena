package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.showCustomTabsIntent
import com.suihan74.utilities.showToast
import com.suihan74.utilities.withArguments
import kotlinx.coroutines.*

/** メニュー項目 */
private typealias MenuItem = Triple<Int, suspend (Context, Entry?, String?)->Unit, ((Entry)->Boolean)?>

/** メニュー項目の追加 */
private fun MutableList<MenuItem>.add(
    textId: Int,
    action: suspend (Context,Entry?,String?)->Unit,
    checker: ((Entry)->Boolean)? = null
) {
    add(Triple(textId, action, checker))
}

/** エントリメニューダイアログ */
class EntryMenuDialog : DialogFragment() {
    /** メニュー項目 */
    @OptIn(ExperimentalStdlibApi::class)
    private val menuItems = buildList<MenuItem> {
        add(R.string.entry_action_show_comments, { context, entry, url -> showBookmarks(context, entry, url) })
        add(R.string.entry_action_show_page, { context, entry, url -> showPage(context, entry, url) })
        add(R.string.entry_action_show_page_in_browser, { context, entry, url -> showPageInBrowser(context, entry, url) })
        add(R.string.entry_action_show_entries, { context, entry, url -> showEntries(context, entry, url) })
        add(R.string.entry_action_ignore, { context, entry, url -> ignoreSite(context, entry, url) })
        add(R.string.bookmark_remove, { context, entry, url -> removeBookmark(context, entry, url) }, { entry -> entry.bookmarkedData != null })
    }

    companion object {
        fun createInstance(entry: Entry) = EntryMenuDialog().withArguments {
            putSerializable(ARG_ENTRY, entry)
        }

        fun createInstance(url: String) = EntryMenuDialog().withArguments {
            putString(ARG_ENTRY_URL, url)
        }

        /** タップ/ロングタップ時の挙動を処理する */
        fun act(entry: Entry, actionEnum: TapEntryAction, fragmentManager: FragmentManager, tag: String? = null) {
            val instance = createInstance(entry)
            when (actionEnum) {
                TapEntryAction.SHOW_MENU ->
                    instance.show(fragmentManager, tag)

                else ->
                    act(
                        instance,
                        entry,
                        actionEnum,
                        fragmentManager,
                        tag
                    )
            }
        }

        /** タップ/ロングタップ時の挙動を処理する */
        fun act(url: String, actionEnum: TapEntryAction, fragmentManager: FragmentManager, tag: String? = null) {
            val instance = createInstance(url)
            when (actionEnum) {
                TapEntryAction.SHOW_MENU ->
                    instance.show(fragmentManager, tag)

                else ->
                    act(
                        instance,
                        url,
                        actionEnum,
                        fragmentManager,
                        tag
                    )
            }
        }

        /** タップ/ロングタップ時の挙動を処理する(メニュー表示以外の挙動) */
        private fun act(instance: EntryMenuDialog, entry: Entry, actionEnum: TapEntryAction, fragmentManager: FragmentManager, tag: String? = null) {
            fragmentManager.beginTransaction()
                .add(instance, tag)
                .runOnCommit {
                    val action = instance.menuItems.firstOrNull { it.first == actionEnum.titleId }
                    GlobalScope.launch(Dispatchers.Main) {
                        action?.second?.invoke(SatenaApplication.instance.applicationContext, entry, null)
                    }

                    fragmentManager.beginTransaction()
                        .remove(instance)
                        .commit()
                }.commit()
        }

        /** タップ/ロングタップ時の挙動を処理する(メニュー表示以外の挙動) */
        private fun act(instance: EntryMenuDialog, url: String, actionEnum: TapEntryAction, fragmentManager: FragmentManager, tag: String? = null) {
            fragmentManager.beginTransaction()
                .add(instance, tag)
                .runOnCommit {
                    val action = instance.menuItems.firstOrNull { it.first == actionEnum.titleId }
                    GlobalScope.launch(Dispatchers.Main) {
                        action?.second?.invoke(SatenaApplication.instance.applicationContext, null, url)
                    }

                    fragmentManager.beginTransaction()
                        .remove(instance)
                        .commit()
                }.commit()
        }

        /** (はてなから取得できた完全な)エントリ */
        private const val ARG_ENTRY = "ARG_ENTRY"

        /** エントリが得られない場合のURL */
        private const val ARG_ENTRY_URL = "ARG_ENTRY_URL"

        /** エントリ非表示設定ダイアログ */
        private const val DIALOG_IGNORE_SITE = "DIALOG_IGNORE_SITE"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val arguments = requireArguments()

        val url = arguments.getString(ARG_ENTRY_URL)
        val entry = (arguments.get(ARG_ENTRY) as? Entry) ?: Entry(
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
        val inflater = LayoutInflater.from(context)
        val titleViewBinding = DataBindingUtil.inflate<DialogTitleEntry2Binding>(
            inflater,
            R.layout.dialog_title_entry2,
            null,
            false
        ).also {
            it.entry = entry
        }

        // メニューに表示する項目リストを作成する
        val activeItems = menuItems.filter { it.third?.invoke(entry) != false }
        val activeItemLabels = activeItems.map { getString(it.first) }.toTypedArray()

        val activity = requireActivity()
        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(activeItemLabels) { _, which ->
                lifecycleScope.launch(Dispatchers.Main + SupervisorJob()) {
                    activeItems[which].second.invoke(activity, entry, url)
                }
            }
            .create()
    }

    /** ブックマーク画面に遷移 */
    private fun showBookmarks(context: Context, entry: Entry?, url: String?) {
        val intent = Intent(context, BookmarksActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
            if (entry != null) putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
            if (url != null) putExtra(BookmarksActivity.EXTRA_ENTRY_URL, url)
        }
        context.startActivity(intent)
    }

    /** ページを内部ブラウザで開く */
    private fun showPage(context: Context, entry: Entry?, url: String?) {
        try {
            if (entry != null) context.showCustomTabsIntent(entry)
            if (url != null) context.showCustomTabsIntent(url)
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
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(extraUrl)
                addFlags(FLAG_ACTIVITY_NEW_TASK)
            }

            checkNotNull(intent.resolveActivity(context.packageManager)) { "cannot resolve intent for browsing the website: $extraUrl" }
            if (extraUrl.startsWith("https://b.hatena.ne.jp/entry/")) {
                context.startActivity(Intent.createChooser(intent, null))
            }
            else {
                context.startActivity(intent)
            }
        }
        catch (e: Throwable) {
            Log.e("browser", Log.getStackTraceString(e))
            context.showToast(R.string.msg_show_page_in_browser_failed)
        }
    }

    /** サイトのエントリリストを開く */
    private fun showEntries(context: Context, entry: Entry?, url: String?) {
        val siteUrl = entry?.rootUrl ?: url!!
        when (val activity = requireActivity() as? EntriesActivity) {
            null -> {
                // EntriesActivity以外から呼ばれた場合、Activityを遷移する
                val intent = Intent(context, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_SITE_URL, siteUrl)
                }
                startActivity(intent)
            }

            else -> {
                activity.showSiteEntries(siteUrl)
            }
        }
    }

    /** サイトを非表示に設定する */
    private fun ignoreSite(context: Context, entry: Entry?, url: String?) {
        val siteUrl = entry?.url ?: url ?: return
        val dialog = IgnoredEntryDialogFragment.createInstance(
            url = siteUrl,
            title = entry?.title ?: "",
            positiveAction = { dialog, ignoredEntry ->
                lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        withContext(Dispatchers.IO) {
                            val dao = SatenaApplication.instance.ignoredEntryDao
                            dao.insert(ignoredEntry)
                        }

                        context.showToast(R.string.msg_ignored_entry_dialog_succeeded, ignoredEntry.query)
                        dialog.dismiss()
                    }
                    catch (e: Throwable) {
                        context.showToast(R.string.msg_ignored_entry_dialog_failed)
                    }
                }
                false
            }
        )
        dialog.show(parentFragmentManager, DIALOG_IGNORE_SITE)
    }

    /** ブクマを削除する */
    private suspend fun removeBookmark(context: Context, entry: Entry?, url: String?) {
        try {
            val target = entry?.url ?: url ?: throw RuntimeException("failed to remove a bookmark")
            // TODO: 「あとで読む」が削除できないっぽい
            HatenaClient.deleteBookmarkAsync(target).await()
            context.showToast(R.string.msg_remove_bookmark_succeeded)
        }
        catch (e: Throwable) {
            context.showToast(R.string.msg_remove_bookmark_failed)
            Log.e("EntryMenuDialog", Log.getStackTraceString(e))
        }
    }
}
