package com.suihan74.satena.scenes.preferences.favoriteSites

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
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel

typealias FavoriteSiteMenuListener = (site: FavoriteSite, f: FavoriteSiteMenuDialog)->Unit

class FavoriteSiteMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(targetSite: FavoriteSite) = FavoriteSiteMenuDialog().withArguments {
            putObject(ARG_TARGET_SITE, targetSite)
        }

        private const val ARG_TARGET_SITE = "ARG_TARGET_SITE"
    }

    private val viewModel by lazyProvideViewModel {
        val targetSite = requireArguments().getObject<FavoriteSite>(ARG_TARGET_SITE)!!
        DialogViewModel(requireContext(), targetSite)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // カスタムタイトルを生成
        val titleViewBinding = DataBindingUtil.inflate<DialogTitleEntry2Binding>(
            localLayoutInflater(),
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

        return createBuilder()
            .setCustomTitle(titleViewBinding.root)
            .setItems(viewModel.labels) { _, which ->
                viewModel.invokeAction(which, this)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
    }

    // ------ //

    fun setOnOpenListener(listener: FavoriteSiteMenuListener?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpen = listener
    }

    fun setOnModifyListener(listener: FavoriteSiteMenuListener?) = lifecycleScope.launchWhenCreated {
        viewModel.onModify = listener
    }

    fun setOnOpenEntriesListener(listener: FavoriteSiteMenuListener?) = lifecycleScope.launchWhenCreated {
        viewModel.onOpenEntries = listener
    }

    fun setOnDeleteListener(listener: FavoriteSiteMenuListener?) = lifecycleScope.launchWhenCreated {
        viewModel.onDelete = listener
    }

    // ------ //

    class DialogViewModel(
        val context: Context,
        val targetSite: FavoriteSite
    ) : ViewModel() {
        /** メニュー項目と対応するイベント */
        val menuItems = listOf<Pair<Int, (FavoriteSiteMenuDialog)->Unit>>(
            R.string.dialog_favorite_sites_open to { onOpen?.invoke(targetSite, it) },
            R.string.dialog_favorite_sites_open_entries to { onOpenEntries?.invoke(targetSite, it) },
            R.string.dialog_favorite_sites_modify to { onModify?.invoke(targetSite, it) },
            R.string.dialog_favorite_sites_delete to { onDelete?.invoke(targetSite, it) }
        )

        /** メニュー表示項目 */
        val labels = menuItems.map { context.getString(it.first) }.toTypedArray()

        /** 対象アイテムを内部ブラウザで開く */
        var onOpen: FavoriteSiteMenuListener? = null

        /** 対象アイテムを編集する */
        var onModify: FavoriteSiteMenuListener? = null

        /** 対象サイトのエントリ一覧を開く */
        var onOpenEntries: FavoriteSiteMenuListener? = null

        /** 対象アイテムを削除 */
        var onDelete: FavoriteSiteMenuListener? = null

        fun invokeAction(which: Int, dialogFragment: FavoriteSiteMenuDialog) {
            menuItems[which].second.invoke(dialogFragment)
        }
    }
}
