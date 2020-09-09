package com.suihan74.satena.scenes.entries2.pages

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments

class FavoriteSitesFragment : MultipleTabsEntriesFragment() {
    companion object {
        fun createInstance() = FavoriteSitesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.FavoriteSites)
        }
    }

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ): EntriesFragmentViewModel {
        return ViewModelProvider(owner)[viewModelKey, FavoriteSitesViewModel::class.java]
    }
}
