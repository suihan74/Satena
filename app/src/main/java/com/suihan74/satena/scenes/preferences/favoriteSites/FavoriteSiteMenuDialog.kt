package com.suihan74.satena.scenes.preferences.favoriteSites

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.whenStarted
import com.suihan74.satena.R
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.utilities.Listener
import com.suihan74.utilities.getObject
import com.suihan74.utilities.putObject
import com.suihan74.utilities.withArguments

class FavoriteSiteMenuDialog : DialogFragment() {
    companion object {
        fun createInstance(targetSite: FavoriteSite) = FavoriteSiteMenuDialog().withArguments {
            putObject(ARG_TARGET_SITE, targetSite)
        }

        private const val ARG_TARGET_SITE = "ARG_TARGET_SITE"
    }

    private val viewModel by lazy {
        val targetSite = requireArguments().getObject<FavoriteSite>(ARG_TARGET_SITE)!!
        val factory = DialogViewModel.Factory(requireContext(), targetSite)
        ViewModelProvider(this, factory)[DialogViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(viewModel.targetSite.title)
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

        class Factory(
            private val context: Context,
            private val targetSite: FavoriteSite
        ) : ViewModelProvider.NewInstanceFactory() {
            @Suppress("unchecked_cast")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                DialogViewModel(context, targetSite) as T
        }
    }
}
