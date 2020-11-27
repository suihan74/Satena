package com.suihan74.satena.scenes.browser

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.models.browser.HistoryPage
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel

class BackStackItemMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(page: HistoryPage) = BackStackItemMenuDialog().withArguments {
            putObject(ARG_TARGET_PAGE, page)
        }

        private const val ARG_TARGET_PAGE = "ARG_TARGET_PAGE"
    }

    private val viewModel by lazy {
        provideViewModel(this) {
            val page = requireArguments().getObject<HistoryPage>(ARG_TARGET_PAGE)!!
            DialogViewModel(requireContext(), page)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // カスタムタイトルを生成
        val titleViewBinding = DataBindingUtil.inflate<DialogTitleEntry2Binding>(
            localLayoutInflater(),
            R.layout.dialog_title_entry2,
            null,
            false
        ).also { binding ->
            viewModel.page.let { page ->
                binding.title = page.title
                binding.url = page.url
                binding.rootUrl = page.url
                binding.faviconUrl = page.faviconUrl
            }
        }

        return createBuilder()
            .setCustomTitle(titleViewBinding.root)
            .setItems(viewModel.labels) { _, which ->
                viewModel.invokeAction(which)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
    }

    // ------ //

    fun setOnOpenListener(listener: Listener<HistoryPage>?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpen = listener
    }

    fun setOnOpenBookmarksListener(listener: Listener<HistoryPage>?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpenBookmarks = listener
    }

    fun setOnOpenEntriesListener(listener: Listener<HistoryPage>?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpenEntries = listener
    }

    // ------ //

    class DialogViewModel(
        val context: Context,
        val page: HistoryPage
    ) : ViewModel() {
        /** メニュー項目と対応するイベント */
        private val menuItems = listOf(
            R.string.dialog_history_open to { onOpen?.invoke(page) },
            R.string.dialog_history_open_bookmarks to { onOpenBookmarks?.invoke(page) },
            R.string.dialog_history_open_entries to { onOpenEntries?.invoke(page) },
        )

        /** メニュー表示項目 */
        val labels = menuItems.map { context.getString(it.first) }.toTypedArray()

        /** 対象アイテムを内部ブラウザで開く */
        var onOpen : Listener<HistoryPage>? = null

        /** ブクマ一覧画面を開く */
        var onOpenBookmarks : Listener<HistoryPage>? = null

        /** 対象サイトのエントリ一覧を開く */
        var onOpenEntries : Listener<HistoryPage>? = null

        fun invokeAction(which: Int) {
            menuItems[which].second.invoke()
        }
    }
}
