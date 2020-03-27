package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** エントリメニューダイアログ */
class EntryMenuDialog : DialogFragment() {
    /** メニュー項目 */
    @OptIn(ExperimentalStdlibApi::class)
    private val menuItems = buildList<Pair<Int, (Entry?,String?)->Unit>> {
        add(R.string.entry_action_show_comments to { entry, url -> showBookmarks(entry, url) })
        add(R.string.entry_action_show_page to { entry, url -> showPage(entry, url) })
        add(R.string.entry_action_show_page_in_browser to { entry, url -> showPageInBrowser(entry, url) })
        add(R.string.entry_action_show_entries to { entry, url -> showEntries(entry, url) })
        add(R.string.entry_action_ignore to { entry, url -> ignoreSite(entry, url) })
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
                    action?.second?.invoke(entry, null)

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
                    action?.second?.invoke(null, url)

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
        val entry = (arguments.get(ARG_ENTRY) as? Entry)

        // カスタムタイトルを生成
        val inflater = LayoutInflater.from(context)
        val titleViewBinding = DataBindingUtil.inflate<DialogTitleEntry2Binding>(
            inflater,
            R.layout.dialog_title_entry2,
            null,
            false
        ).apply {
            this.entry = entry ?: Entry(
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
        }

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(menuItems.map { getString(it.first) }.toTypedArray()) { _, which ->
                menuItems[which].second.invoke(entry, url)
            }
            .create()
    }

    /** ブックマーク画面に遷移 */
    private fun showBookmarks(entry: Entry?, url: String?) {
        val intent = Intent(requireContext(), BookmarksActivity::class.java).apply {
            if (entry != null) putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
            if (url != null) putExtra(BookmarksActivity.EXTRA_ENTRY_URL, url)
        }
        startActivity(intent)
    }

    /** ページを内部ブラウザで開く */
    private fun showPage(entry: Entry?, url: String?) {
        if (entry != null) requireContext().showCustomTabsIntent(entry)
        if (url != null) requireContext().showCustomTabsIntent(url)
    }

    /** ページを外部ブラウザで開く */
    private fun showPageInBrowser(entry: Entry?, url: String?) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            if (entry != null) Uri.parse(entry.url)
            else Uri.parse(url!!)
        )

        startActivity(intent)
    }

    /** サイトのエントリリストを開く */
    private fun showEntries(entry: Entry?, url: String?) {
        val siteUrl = entry?.rootUrl ?: url!!
        when (val activity = requireActivity() as? EntriesActivity) {
            null -> {
                // EntriesActivity以外から呼ばれた場合、Activityを遷移する
                val intent = Intent(requireContext(), EntriesActivity::class.java).apply {
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
    private fun ignoreSite(entry: Entry?, url: String?) {
        val siteUrl = entry?.url ?: url ?: return
        val activity = requireActivity()
        val dialog = IgnoredEntryDialogFragment.createInstance(
            url = siteUrl,
            title = entry?.title ?: "",
            positiveAction = { dialog, ignoredEntry ->
                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        withContext(Dispatchers.IO) {
                            val dao = SatenaApplication.instance.ignoredEntryDao
                            dao.insert(ignoredEntry)
                        }

                        activity.showToast(R.string.msg_ignored_entry_dialog_succeeded, ignoredEntry.query)
                        dialog.dismiss()
                    }
                    catch (e: Throwable) {
                        activity.showToast(R.string.msg_ignored_entry_dialog_failed)
                    }
                }
                false
            }
        )
        dialog.show(parentFragmentManager, DIALOG_IGNORE_SITE)
    }
}
