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
    private val entriesType: EntriesType
) : ViewModel() {
    /** タブで表示するエントリーリスト */
    val items by lazy {
        MutableLiveData<List<Entry>>()
    }

    fun init(onError: ((Throwable)->Unit)? = null) = viewModelScope.launch(Dispatchers.Main) {
        try {
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
        private val entriesType: EntriesType
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            EntriesTabFragmentViewModel(repository, category, entriesType) as T
    }
}
