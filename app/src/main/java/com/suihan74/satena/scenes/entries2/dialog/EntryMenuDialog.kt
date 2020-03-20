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
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.showCustomTabsIntent
import com.suihan74.utilities.withArguments

/** エントリメニューダイアログ */
class EntryMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(entry: Entry) = EntryMenuDialog().withArguments {
            putSerializable(ARG_ENTRY, entry)
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

        /** タップ/ロングタップ時の挙動を処理する(メニュー表示以外の挙動) */
        private fun act(instance: EntryMenuDialog, entry: Entry, actionEnum: TapEntryAction, fragmentManager: FragmentManager, tag: String? = null) {
            fragmentManager.beginTransaction()
                .add(instance, tag)
                .runOnCommit {
                    val action = instance.menuItems.firstOrNull { it.first == actionEnum.titleId }
                    action?.second?.invoke(entry)

                    fragmentManager.beginTransaction()
                        .remove(instance)
                        .commit()
                }.commit()
        }

        private const val ARG_ENTRY = "ARG_ENTRY"
    }

    /** メニュー項目 */
    @OptIn(ExperimentalStdlibApi::class)
    private val menuItems = buildList<Pair<Int, (Entry)->Unit>> {
        add(R.string.entry_action_show_comments to { entry -> showBookmarks(entry) })
        add(R.string.entry_action_show_page to { entry -> showPage(entry) })
        add(R.string.entry_action_show_page_in_browser to { entry -> showPageInBrowser(entry) })
        add(R.string.entry_action_show_entries to { entry -> showEntries(entry) })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val arguments = requireArguments()

        val entry = arguments.get(ARG_ENTRY) as Entry

        // カスタムタイトルを生成
        val inflater = LayoutInflater.from(context)
        val titleViewBinding = DataBindingUtil.inflate<DialogTitleEntry2Binding>(
            inflater,
            R.layout.dialog_title_entry2,
            null,
            false
        ).apply {
            this.entry = entry
        }

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(menuItems.map { getString(it.first) }.toTypedArray()) { _, which ->
                menuItems[which].second.invoke(entry)
            }
            .create()
    }

    /** ブックマーク画面に遷移 */
    private fun showBookmarks(entry: Entry) {
        val intent = Intent(requireContext(), BookmarksActivity::class.java).apply {
            putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
        }
        startActivity(intent)
    }

    /** ページを内部ブラウザで開く */
    private fun showPage(entry: Entry) {
        requireContext().showCustomTabsIntent(entry)
    }

    /** ページを外部ブラウザで開く */
    private fun showPageInBrowser(entry: Entry) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(entry.url))
        startActivity(intent)
    }

    /** サイトのエントリリストを開く */
    private fun showEntries(entry: Entry) {
        when (val activity = requireActivity() as? EntriesActivity) {
            null -> {
                // EntriesActivity以外から呼ばれた場合、Activityを遷移する
                val intent = Intent(requireContext(), EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_SITE_URL, entry.rootUrl)
                }
                startActivity(intent)
            }

            else -> {
                activity.showSiteEntries(entry.rootUrl)
            }
        }
    }
}
