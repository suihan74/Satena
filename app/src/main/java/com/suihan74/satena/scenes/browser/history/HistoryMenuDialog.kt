package com.suihan74.satena.scenes.browser.history

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.models.browser.History
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel

class HistoryMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(targetSite: History) = HistoryMenuDialog().withArguments {
            putObject(ARG_TARGET_SITE, targetSite)
        }

        private const val ARG_TARGET_SITE = "ARG_TARGET_SITE"
    }

    private val viewModel by lazy {
        provideViewModel(this) {
            val targetSite = requireArguments().getObject<History>(ARG_TARGET_SITE)!!
            DialogViewModel(requireContext(), targetSite)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // カスタムタイトルを生成
        val inflater = LayoutInflater.from(context)
        val titleViewBinding = DataBindingUtil.inflate<DialogTitleEntry2Binding>(
            inflater,
            R.layout.dialog_title_entry2,
            null,
            false
        ).also {
            val site = viewModel.targetSite
            it.title = site.title
            it.url = site.url
            it.rootUrl = site.url
            it.faviconUrl = site.faviconUrl
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setCustomTitle(titleViewBinding.root)
            .setItems(viewModel.labels) { _, which ->
                viewModel.invokeAction(which)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
    }

    // ------ //

    fun setOnOpenListener(listener: Listener<History>?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpen = listener
    }

    fun setOnOpenEntriesListener(listener: Listener<History>?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpenEntries = listener
    }

    fun setOnFavoriteListener(listener: Listener<History>?) = lifecycleScope.launchWhenCreated {
        viewModel.onFavorite = listener
    }

    fun setOnDeleteListener(listener: Listener<History>?) = lifecycleScope.launchWhenCreated {
        viewModel.onDelete = listener
    }

    // ------ //

    class DialogViewModel(
        val context: Context,
        val targetSite: History
    ) : ViewModel() {
        /** メニュー項目と対応するイベント */
        val menuItems = listOf(
            R.string.dialog_history_open to { onOpen?.invoke(targetSite) },
            R.string.dialog_history_open_entries to { onOpenEntries?.invoke(targetSite) },
            R.string.dialog_history_delete to { onDelete?.invoke(targetSite) }
        )

        /** メニュー表示項目 */
        val labels = menuItems.map { context.getString(it.first) }.toTypedArray()

        /** 対象アイテムを内部ブラウザで開く */
        var onOpen : Listener<History>? = null

        /** 対象サイトのエントリ一覧を開く */
        var onOpenEntries : Listener<History>? = null

        /** お気に入りに追加 */
        var onFavorite : Listener<History>? = null

        /** 対象アイテムを削除 */
        var onDelete : Listener<History>? = null

        fun invokeAction(which: Int) {
            menuItems[which].second.invoke()
        }
    }
}
