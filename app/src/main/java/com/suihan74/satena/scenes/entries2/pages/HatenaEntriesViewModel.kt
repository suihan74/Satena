package com.suihan74.satena.scenes.entries2.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository

class HatenaEntriesViewModel(
    private val repository: EntriesRepository
) : EntriesFragmentViewModel() {
    override val tabTitles = arrayOf(
        R.string.entries_tab_hot,
        R.string.entries_tab_recent
    )

    /** 現在categoryが内包するissueのリスト */
    val issues by lazy {
        repository.IssuesLiveData(category)
    }

    class Factory(
        private val repository: EntriesRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            HatenaEntriesViewModel(repository) as T
    }
}
