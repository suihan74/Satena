package com.suihan74.satena.scenes.preferences.favoriteSites

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.models.favoriteSite.FavoriteSiteAndFavicon
import com.suihan74.utilities.SuspendDialogListener
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoriteSiteMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(targetSite: FavoriteSiteAndFavicon) = FavoriteSiteMenuDialog().withArguments {
            putObject(ARG_TARGET_SITE, targetSite)
        }

        private const val ARG_TARGET_SITE = "ARG_TARGET_SITE"
    }

    private val viewModel by lazyProvideViewModel {
        val targetSite = requireArguments().getObject<FavoriteSiteAndFavicon>(ARG_TARGET_SITE)!!
        DialogViewModel(targetSite)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        // カスタムタイトルを生成
        val titleViewBinding = DialogTitleEntry2Binding.inflate(localLayoutInflater(), null, false).also {
            val target = viewModel.target
            it.title = target.site.title
            it.url = target.site.url
            it.rootUrl = target.site.url
            it.faviconUrl =
                target.faviconInfo?.filename?.let { filename ->
                    "${context.filesDir}/favicon_cache/$filename"
                } ?: target.site.faviconUrl
        }

        return createBuilder()
            .setCustomTitle(titleViewBinding.root)
            .setItems(viewModel.labels(context), null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
            .apply {
                listView.setOnItemClickListener { adapterView, view, i, l ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        runCatching {
                            viewModel.invokeAction(i, this@FavoriteSiteMenuDialog)
                        }
                        runCatching {
                            dismiss()
                        }
                    }
                }
            }
    }

    // ------ //

    fun setOnOpenListener(listener: SuspendDialogListener<FavoriteSiteAndFavicon>?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpen = listener
    }

    fun setOnModifyListener(listener: SuspendDialogListener<FavoriteSiteAndFavicon>?) = lifecycleScope.launchWhenCreated {
        viewModel.onModify = listener
    }

    fun setOnOpenEntriesListener(listener: SuspendDialogListener<FavoriteSiteAndFavicon>?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpenEntries = listener
    }

    fun setOnDeleteListener(listener: SuspendDialogListener<FavoriteSiteAndFavicon>?) = lifecycleScope.launchWhenCreated {
        viewModel.onDelete = listener
    }

    // ------ //

    class DialogViewModel(
        val target: FavoriteSiteAndFavicon
    ) : ViewModel() {
        /** メニュー項目と対応するイベント */
        private val menuItems = listOf<Pair<Int, suspend (FavoriteSiteMenuDialog)->Unit>>(
            R.string.dialog_favorite_sites_open to { onOpen?.invoke(target, it) },
            R.string.dialog_favorite_sites_open_entries to { onOpenEntries?.invoke(target, it) },
            R.string.dialog_favorite_sites_modify to { onModify?.invoke(target, it) },
            R.string.dialog_favorite_sites_delete to { onDelete?.invoke(target, it) }
        )

        /** メニュー表示項目 */
        fun labels(context: Context) = menuItems.map { context.getString(it.first) }.toTypedArray()

        /** 対象アイテムを内部ブラウザで開く */
        var onOpen: SuspendDialogListener<FavoriteSiteAndFavicon>? = null

        /** 対象アイテムを編集する */
        var onModify: SuspendDialogListener<FavoriteSiteAndFavicon>? = null

        /** 対象サイトのエントリ一覧を開く */
        var onOpenEntries: SuspendDialogListener<FavoriteSiteAndFavicon>? = null

        /** 対象アイテムを削除 */
        var onDelete: SuspendDialogListener<FavoriteSiteAndFavicon>? = null

        suspend fun invokeAction(which: Int, dialogFragment: FavoriteSiteMenuDialog) {
            menuItems[which].second.invoke(dialogFragment)
        }
    }
}
