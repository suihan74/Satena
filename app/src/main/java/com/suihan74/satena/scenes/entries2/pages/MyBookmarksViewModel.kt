package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.*
import com.suihan74.utilities.OnError
import com.suihan74.utilities.SingleUpdateMutableLiveData

class MyBookmarksViewModel(
    private val repository : EntriesRepository
) : EntriesFragmentViewModel(), TagsLiveDataContainer {
    /** 内包するタブ */
    private val tabs = EntriesTabType.getTabs(Category.MyBookmarks)

    /** ユーザーのタグ一覧 */
    override val tags by lazy {
        repository.TagsLiveData()
    }

    /** 検索クエリ */
    val searchQuery by lazy {
        SingleUpdateMutableLiveData<String>()
    }

    /** 検索窓の展開状態 */
    var isSearchViewExpanded : Boolean = false

    override val tabCount: Int = tabs.size
    override fun getTabTitle(context: Context, position: Int) : String =
        context.getString(tabs[position].textId)

    /** タブ用ViewModelへの値変更の伝播 */
    override fun connectToTab(
        lifecycleOwner: LifecycleOwner,
        entriesAdapter: EntriesAdapter,
        viewModel: EntriesTabFragmentViewModel,
        onError: OnError?
    ) {
        super.connectToTab(lifecycleOwner, entriesAdapter, viewModel, onError)
        searchQuery.observe(lifecycleOwner, {
            viewModel.searchQuery = it
        })
    }
}
