package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.whenStarted
import com.suihan74.satena.R
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel

/** 有効なお気に入りサイトを選択するダイアログ */
// TODO: BottomSheetDialog化する？
// TODO: タイトル・URL両方を表示する
class FavoriteSitesSelectionDialog : DialogFragment() {
    companion object {
        fun createInstance(sites: List<FavoriteSite>) = FavoriteSitesSelectionDialog().withArguments {
            putObject(ARG_SITES, sites)
        }

        private const val ARG_SITES = "ARG_SITES"
    }

    private val viewModel: DialogViewModel by lazy {
        provideViewModel(this) {
            val sites = requireArguments().getObject<List<FavoriteSite>>(ARG_SITES)!!
            DialogViewModel(sites)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.desc_favorite_sites_settings)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.invokeOnComplete()
            }
            .setMultiChoiceItems(viewModel.labels, viewModel.checkedItems) { _, which, checked ->
                viewModel.checkedItems[which] = checked
            }
            .create()
    }

    /** 完了時処理をセット */
    suspend fun setOnCompleteListener(listener: Listener<List<FavoriteSite>>?) = whenStarted {
        viewModel.onComplete = listener
    }

    // ------ //

    class DialogViewModel(
        /** 登録されているサイト設定一覧 */
        val sites: List<FavoriteSite>
    ): ViewModel() {
        /** チェック状態 */
        val checkedItems: BooleanArray by lazy {
            sites.map { it.isEnabled }.toBooleanArray()
        }

        /** リスト項目の表示内容 */
        val labels: Array<String> by lazy {
            sites.map { it.title }.toTypedArray()
        }

        /**
         * 決定時の処理
         *
         * 新しい有効状態が反映されたFavoriteSiteリストを渡す
         */
        var onComplete: Listener<List<FavoriteSite>>? = null

        fun invokeOnComplete() {
            val newList = sites.mapIndexed { idx, value ->
                if (checkedItems[idx] == value.isEnabled) value
                else value.copy(isEnabled = checkedItems[idx])
            }
            onComplete?.invoke(newList)
        }
    }
}
