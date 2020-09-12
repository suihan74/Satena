package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesAdapter
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentViewModel
import com.suihan74.satena.scenes.entries2.dialog.FavoriteSitesSelectionDialog
import com.suihan74.utilities.OnError
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoriteSitesViewModel(
    private val repository: EntriesRepository
) : EntriesFragmentViewModel() {
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
        val sites = repository.favoriteSites.value ?: emptyList()

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

    // ------ //

    class Factory(
        private val repository: EntriesRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            FavoriteSitesViewModel(repository) as T
    }
}
