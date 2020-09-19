package com.suihan74.satena.scenes.entries2.pages

import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.utilities.extensions.putEnum
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel

class StarsFragment : MultipleTabsEntriesFragment() {
    companion object {
        fun createInstance() = StarsFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.Stars)
        }
    }

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ) = provideViewModel(owner, viewModelKey) {
        StarsViewModel(repository)
    }
}
