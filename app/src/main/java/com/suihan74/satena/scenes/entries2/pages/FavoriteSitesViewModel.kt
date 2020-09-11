package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.models.FavoriteSitesKey
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.dialog.FavoriteSitesSelectionDialog
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoriteSitesViewModel : EntriesFragmentViewModel() {
    private val tabTitles = arrayOf(
        R.string.entries_tab_hot,
        R.string.entries_tab_recent
    )

    override val tabCount: Int = 2
    override fun getTabTitle(context: Context, position: Int): String =
        context.getString(tabTitles[position])

    /** 表示するサイトを選択するダイアログを開く */
    fun showSitesSelectionDialog(
        context: Context,
        fragmentManager: FragmentManager,
        tag: String? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        // TODO: リポジトリでやる
        val prefs = SafeSharedPreferences.create<FavoriteSitesKey>(context)
        val sites = prefs.get<List<FavoriteSite>>(FavoriteSitesKey.SITES)

        FavoriteSitesSelectionDialog.createInstance(sites).run {
            showAllowingStateLoss(fragmentManager, tag)

            setOnCompleteListener { newList ->
                prefs.edit {
                    put(FavoriteSitesKey.SITES, newList)
                }
            }
        }
    }
}
