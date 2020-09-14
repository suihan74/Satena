package com.suihan74.satena.scenes.preferences.favoriteSites

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.whenStarted
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.utilities.*

class FavoriteSiteMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(targetSite: FavoriteSite) = FavoriteSiteMenuDialog().withArguments {
            putObject(ARG_TARGET_SITE, targetSite)
        }

        private const val ARG_TARGET_SITE = "ARG_TARGET_SITE"
    }

    private val viewModel by lazy {
        provideViewModel(this) {
            val targetSite = requireArguments().getObject<FavoriteSite>(ARG_TARGET_SITE)!!
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

    suspend fun setOnDeleteListener(listener: Listener<FavoriteSite>?) = whenStarted {
        viewModel.onDelete = listener
    }

    // ------ //

    class DialogViewModel(
        val context: Context,
        val targetSite: FavoriteSite
    ) : ViewModel() {
        /** メニュー項目と対応するイベント */
        val menuItems = listOf(
            R.string.dialog_delete to { onDelete?.invoke(targetSite) }
        )

        /** メニュー表示項目 */
        val labels = menuItems.map { context.getString(it.first) }.toTypedArray()

        /** 対象アイテムを削除 */
        var onDelete: Listener<FavoriteSite>? = null

        fun invokeAction(which: Int) {
            menuItems[which].second.invoke()
        }
    }
}
