package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.models.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntriesTabFragmentViewModel(
    private val repository: EntriesRepository,
    private val category: Category,
    private val tabPosition: Int
) : ViewModel() {
    /** フィルタされていない全エントリリスト */
    private val items by lazy {
        MutableLiveData<List<Entry>>()
    }

    /** タブで表示するエントリリスト */
    val filteredItems by lazy {
        MutableLiveData<List<Entry>>().also { filtered ->
            items.observeForever {
                viewModelScope.launch {
                    filtered.postValue(repository.filterEntries(it))
                }
            }
        }
    }

    /** エントリリストを初期化 */
    fun refresh(onError: ((Throwable)->Unit)? = null) = viewModelScope.launch(Dispatchers.Main) {
        try {
            val entries = repository.loadEntries(category, tabPosition)
            items.value = entries
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }
    }

    /** エントリリストの追加ロード */
    fun loadAdditional(
        onFinally: ((Throwable?)->Unit)? = null,
        onError: ((Throwable)->Unit)? = null
    ) = viewModelScope.launch(Dispatchers.Default) {
        var error : Throwable? = null
        try {
            val offset = items.value?.size ?: 0
            val entries = repository.loadEntries(category, tabPosition, offset)
            val oldItems = items.value ?: emptyList()
            items.postValue(
                oldItems.plus(entries).distinctBy { it.id }
            )
        }
        catch (e: Throwable) {
            error = e
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }
        finally {
            withContext(Dispatchers.Main) {
                onFinally?.invoke(error)
            }
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
