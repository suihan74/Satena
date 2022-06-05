package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.models.favoriteSite.FavoriteSiteAndFavicon
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 有効なお気に入りサイトを選択するダイアログ */
// TODO: BottomSheetDialog化する？
// TODO: タイトル・URL両方を表示する
class FavoriteSitesSelectionDialog : DialogFragment() {
    companion object {
        fun createInstance(sites: List<FavoriteSiteAndFavicon>) = FavoriteSitesSelectionDialog().withArguments {
            putObject(ARG_SITES, sites)
        }

        private const val ARG_SITES = "ARG_SITES"
    }

    private val viewModel: DialogViewModel by lazyProvideViewModel {
        val repo = SatenaApplication.instance.favoriteSitesRepository
        val sites = requireArguments().getObject<List<FavoriteSiteAndFavicon>>(ARG_SITES)!!
        DialogViewModel(repo, sites)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return createBuilder()
            .setTitle(R.string.desc_favorite_sites_settings)
            .setPositiveButton(R.string.dialog_ok, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setMultiChoiceItems(viewModel.labels, viewModel.checkedItems) { _, which, checked ->
                viewModel.checkedItems[which] = checked
            }
            .show()
            .apply {
                getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                    lifecycleScope.launch(Dispatchers.Main) {
                        viewModel.onComplete()
                        dismiss()
                    }
                }
            }
    }

    // ------ //

    class DialogViewModel(
        private val repo : FavoriteSitesRepository,
        /** 登録されているサイト設定一覧 */
        val sites: List<FavoriteSiteAndFavicon>
    ): ViewModel() {
        /** チェック状態 */
        val checkedItems: BooleanArray = sites.map { it.site.isEnabled }.toBooleanArray()


        /** リスト項目の表示内容 */
        val labels: Array<String> = sites.map { it.site.title }.toTypedArray()

        suspend fun onComplete() = withContext(Dispatchers.Default) {
            val newList = sites.mapIndexed { idx, value ->
                if (checkedItems[idx] == value.site.isEnabled) value.site
                else value.site.copy(isEnabled = checkedItems[idx])
            }
            repo.update(newList)
        }
    }
}
