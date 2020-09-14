package com.suihan74.satena.scenes.entries2.pages

import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabFragment
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.utilities.provideViewModel
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments

class HistoryFragment : SingleTabEntriesFragment() {
    companion object {
        fun createInstance() = HistoryFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.History)
        }
    }

    /** コンテンツ部分に表示するフラグメントを生成する */
    override fun generateContentFragment(viewModelKey: String) : EntriesTabFragmentBase =
        EntriesTabFragment.createInstance(viewModelKey, Category.History)

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ) = provideViewModel(owner, viewModelKey) {
        HatenaEntriesViewModel(repository)
    }
}
