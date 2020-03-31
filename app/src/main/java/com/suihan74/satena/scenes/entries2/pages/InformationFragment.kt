package com.suihan74.satena.scenes.entries2.pages

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments

/** 障害情報を表示するページ */
class InformationFragment : SingleTabEntriesFragment() {
    companion object {
        fun createInstance() = InformationFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.Maintenance)
        }
    }

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ): EntriesFragmentViewModel {
        val factory = HatenaEntriesViewModel.Factory(repository)
        return ViewModelProvider(owner, factory)[viewModelKey, HatenaEntriesViewModel::class.java]
    }

    override fun generateContentFragment(viewModelKey: String): EntriesTabFragmentBase =
        InformationTabFragment.createInstance(viewModelKey)
}
