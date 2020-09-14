package com.suihan74.satena.scenes.entries2.pages

import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.utilities.provideViewModel
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
    ) = provideViewModel(owner, viewModelKey) {
        HatenaEntriesViewModel(repository)
    }

    override fun generateContentFragment(viewModelKey: String): EntriesTabFragmentBase =
        InformationTabFragment.createInstance(viewModelKey)
}
