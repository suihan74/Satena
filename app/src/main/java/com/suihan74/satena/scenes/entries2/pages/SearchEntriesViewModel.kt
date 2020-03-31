package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.utilities.SingleUpdateMutableLiveData

class SearchEntriesViewModel(
    private val repository : EntriesRepository
) : EntriesFragmentViewModel() {
    private val tabTitles = arrayOf(
        R.string.entries_tab_hot,
        R.string.entries_tab_recent
    )

    /** 検索クエリ */
    val searchQuery by lazy {
        SingleUpdateMutableLiveData<String>()
    }

    /** 検索タイプ */
    val searchType by lazy {
        SingleUpdateMutableLiveData(SearchType.Text)
    }

    override val tabCount: Int = 2
    override fun getTabTitle(context: Context, position: Int) : String = context.getString(tabTitles[position])

    class Factory(
        private val repository : EntriesRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            SearchEntriesViewModel(repository) as T
    }
}
