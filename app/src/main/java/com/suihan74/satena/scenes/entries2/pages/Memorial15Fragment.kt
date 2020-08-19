package com.suihan74.satena.scenes.entries2.pages

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.utilities.alsoAs
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments

class Memorial15Fragment : TwinTabsEntriesFragment() {
    companion object {
        fun createInstance() = Memorial15Fragment().withArguments {
            putEnum(ARG_CATEGORY, Category.Memorial15th)
        }
    }

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ): EntriesFragmentViewModel {
        val factory = Memorial15ViewModel.Factory(repository)
        return ViewModelProvider(owner, factory)[viewModelKey, Memorial15ViewModel::class.java]
    }

    override fun onResume() {
        super.onResume()

        activity.alsoAs<EntriesActivity> {
            it.tabLayout?.tabMode = TabLayout.MODE_SCROLLABLE
        }
    }
}
