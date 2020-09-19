package com.suihan74.satena.scenes.entries2.pages

import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabFragment
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.utilities.extensions.putEnum
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel

class MyHotEntriesFragment : SingleTabEntriesFragment() {
    companion object {
        fun createInstance() = MyHotEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.MyHotEntries)
        }
    }

    /** コンテンツ部分に表示するフラグメントを生成する */
    override fun generateContentFragment(viewModelKey: String) : EntriesTabFragmentBase =
        EntriesTabFragment.createInstance(viewModelKey, Category.MyHotEntries)

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ) = provideViewModel(owner, viewModelKey) {
        HatenaEntriesViewModel(repository)
    }
}
