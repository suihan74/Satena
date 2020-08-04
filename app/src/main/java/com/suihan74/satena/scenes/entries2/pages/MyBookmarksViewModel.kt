package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.*
import com.suihan74.utilities.SingleUpdateMutableLiveData

class MyBookmarksViewModel(
    private val repository : EntriesRepository
) : EntriesFragmentViewModel(), TagsLiveDataContainer {
    private val tabTitles = arrayOf(
        R.string.entries_tab_mybookmarks,
        R.string.entries_tab_read_later
    )

    /** ユーザーのタグ一覧 */
    override val tags by lazy {
        repository.TagsLiveData()
    }

    /** 検索クエリ */
    val searchQuery by lazy {
        SingleUpdateMutableLiveData<String>()
    }

    override val tabCount: Int = 2
    override fun getTabTitle(context: Context, position: Int) : String =
        context.getString(tabTitles[position])

    /** タブ用ViewModelへの値変更の伝播 */
    override fun connectToTab(
        lifecycleOwner: LifecycleOwner,
        entriesAdapter: EntriesAdapter,
        viewModel: EntriesTabFragmentViewModel,
        onError: ((Throwable) -> Unit)?
    ) {
        super.connectToTab(lifecycleOwner, entriesAdapter, viewModel, onError)
        searchQuery.observe(lifecycleOwner) {
            viewModel.searchQuery = it
        }
    }

    class Factory(
        private val repository : EntriesRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            MyBookmarksViewModel(repository) as T
    }
}
