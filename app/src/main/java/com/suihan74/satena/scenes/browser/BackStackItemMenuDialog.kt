package com.suihan74.satena.scenes.browser

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.webkit.WebHistoryItem
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.utilities.DialogListener
import com.suihan74.utilities.extensions.faviconUrl
import com.suihan74.utilities.lazyProvideViewModel

class BackStackItemMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(page: WebHistoryItem) = BackStackItemMenuDialog().also {
            it.lifecycleScope.launchWhenCreated {
                it.viewModel.page = page
            }
        }
    }

    private val viewModel by lazyProvideViewModel {
        DialogViewModel(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // カスタムタイトルを生成
        val titleViewBinding = DialogTitleEntry2Binding.inflate(
            localLayoutInflater(),
            null,
            false
        ).also { binding ->
            viewModel.page!!.let { page ->
                binding.title = page.title
                binding.url = page.url
                binding.rootUrl = page.url
                binding.faviconUrl = Uri.parse(page.url).faviconUrl
            }
        }

        return createBuilder()
            .setCustomTitle(titleViewBinding.root)
            .setItems(viewModel.labels) { _, which ->
                viewModel.invokeAction(this, which)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
    }

    // ------ //

    fun setOnOpenListener(listener: DialogListener<WebHistoryItem>?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpen = listener
    }

    fun setOnOpenBookmarksListener(listener: DialogListener<WebHistoryItem>?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpenBookmarks = listener
    }

    fun setOnOpenEntriesListener(listener: DialogListener<WebHistoryItem>?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpenEntries = listener
    }

    // ------ //

    class DialogViewModel(val context: Context) : ViewModel() {
        var page: WebHistoryItem? = null

        /** 対象アイテムを内部ブラウザで開く */
        var onOpen : DialogListener<WebHistoryItem>? = null

        /** ブクマ一覧画面を開く */
        var onOpenBookmarks : DialogListener<WebHistoryItem>? = null

        /** 対象サイトのエントリ一覧を開く */
        var onOpenEntries : DialogListener<WebHistoryItem>? = null

        /** メニュー項目と対応するイベント */
        private val menuItems = listOf(
            R.string.dialog_history_open to { onOpen },
            R.string.dialog_history_open_bookmarks to { onOpenBookmarks },
            R.string.dialog_history_open_entries to { onOpenEntries },
        )

        /** メニュー表示項目 */
        val labels = menuItems.map { context.getString(it.first) }.toTypedArray()

        // ------ //

        fun invokeAction(fragment: DialogFragment, which: Int) {
            val action = menuItems[which].second()
            action?.invoke(page!!, fragment)
        }
    }
}
