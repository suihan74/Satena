package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.scenes.entries2.EntriesAdapter
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesTabType
import com.suihan74.satena.scenes.entries2.dialog.FavoriteSitesSelectionDialog
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.utilities.OnError
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
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
        val sites = repository.favoriteSites.value.orEmpty()

        FavoriteSitesSelectionDialog.createInstance(sites).run {
            showAllowingStateLoss(fragmentManager, tag)

            setOnCompleteListener { newList ->
                repository.favoriteSites.value = newList
            }
        }
    }

    override fun connectToTab(
        lifecycleOwner: LifecycleOwner,
        entriesAdapter: EntriesAdapter,
        viewModel: EntriesTabFragmentViewModel,
        onError: OnError?
    ) {
        super.connectToTab(lifecycleOwner, entriesAdapter, viewModel, onError)
        repository.favoriteSites.observe(lifecycleOwner, Observer {
            viewModel.reloadLists()
        })
    }
}
