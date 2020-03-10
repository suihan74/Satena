package com.suihan74.satena.scenes.entries2

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.showCustomTabsIntent

/** エントリメニューダイアログ */
class MenuDialog : DialogFragment() {
    companion object {
        fun createInstance(entry: Entry) = MenuDialog().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_ENTRY, entry)
            }
        }

        private const val ARG_ENTRY = "ARG_ENTRY"
    }

    /** EntriesActivityのViewModel */
    private lateinit var activityViewModel: EntriesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityViewModel = ViewModelProvider(requireActivity())[EntriesViewModel::class.java]
    }

    @ExperimentalStdlibApi
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

        // メニュー項目の作成
        val items = buildList<Pair<Int, (Entry)->Unit>> {
            add(R.string.entry_action_show_comments to { entry -> showBookmarks(entry) })
            add(R.string.entry_action_show_page to { entry -> showPage(entry) })
            add(R.string.entry_action_show_page_in_browser to { entry -> showPageInBrowser(entry) })
        }

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(items.map { getString(it.first) }.toTypedArray()) { _, which ->
                items[which].second.invoke(entry)
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
}
