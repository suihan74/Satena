package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.EntriesType
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.models.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntriesTabFragmentViewModel(
    private val repository: EntriesRepository,
    private val category: Category,
    private val tabPosition: Int
) : ViewModel() {
    /** タブで表示するエントリーリスト */
    val items by lazy {
        MutableLiveData<List<Entry>>()
    }

    fun init(onError: ((Throwable)->Unit)? = null) = viewModelScope.launch(Dispatchers.Main) {
        try {
            val entriesType = when(tabPosition) {
                0 -> EntriesType.Hot
                1 -> EntriesType.Recent
                else -> throw NotImplementedError("invalid tab position")
            }

            val entries = repository.refreshEntries(
                category = category,
                issue = null,
                entriesType = entriesType
            )
            items.postValue(entries)
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }
    }

    class Factory(
        private val repository: EntriesRepository,
        private val category: Category,
        private val tabPosition: Int = 0
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            EntriesTabFragmentViewModel(repository, category, tabPosition) as T
    }
}
