package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.*
import com.suihan74.utilities.OnError
import com.suihan74.utilities.SingleUpdateMutableLiveData

class SearchEntriesViewModel(
    repository : EntriesRepository,
    initialSearchType : SearchType? = null
) : EntriesFragmentViewModel() {
    private val tabTitles = arrayOf(
        R.string.entries_tab_hot,
        R.string.entries_tab_recent
    )

    /** 検索クエリ */
    val searchQuery =
        SingleUpdateMutableLiveData<String>()

    /** 検索設定 */
    val searchSetting = repository.searchSetting.also { liveData ->
        initialSearchType?.let {
            liveData.value = liveData.value?.copy(searchType = it)
        }
    }

    override val tabCount: Int = 2
    override fun getTabTitle(context: Context, position: Int) : String = context.getString(tabTitles[position])

    /** タブ用ViewModelへの値変更の伝播 */
    override fun connectToTab(
        fragment: EntriesTabFragment,
        entriesAdapter: EntriesAdapter,
        viewModel: EntriesTabFragmentViewModel,
        onError: OnError?
    ) {
        super.connectToTab(fragment, entriesAdapter, viewModel, onError)

        // 検索用パラメータの変更を伝播
        searchQuery.observe(fragment.viewLifecycleOwner, {
            viewModel.searchQuery = it
        })
    }
}
