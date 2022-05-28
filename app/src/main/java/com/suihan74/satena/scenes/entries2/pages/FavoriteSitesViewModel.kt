package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.scenes.entries2.*
import com.suihan74.satena.scenes.entries2.dialog.FavoriteSitesSelectionDialog
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.utilities.OnError
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class FavoriteSitesViewModel(
    private val repository: FavoriteSitesRepository
) : EntriesFragmentViewModel() {
    /** 内包するタブ */
    private val tabs = EntriesTabType.getTabs(null)

    override val tabCount: Int = tabs.size
    override fun getTabTitle(context: Context, position: Int): String =
        context.getString(tabs[position].textId)

    /** 表示するサイトを選択するダイアログを開く */
    fun showSitesSelectionDialog(
        fragmentManager: FragmentManager,
        tag: String? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        val sites = repository.allSites()
        FavoriteSitesSelectionDialog.createInstance(sites)
            .showAllowingStateLoss(fragmentManager, tag)
    }

    override fun connectToTab(
        fragment: EntriesTabFragment,
        entriesAdapter: EntriesAdapter,
        viewModel: EntriesTabFragmentViewModel,
        onError: OnError?
    ) {
        super.connectToTab(fragment, entriesAdapter, viewModel, onError)
        repository.favoriteSitesFlow
            .onEach {
                runCatching { viewModel.reloadLists() }
            }.launchIn(fragment.lifecycleScope)
    }
}
