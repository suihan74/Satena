package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.Issue
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.models.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntriesTabFragmentViewModel(
    private val repository: EntriesRepository,
    val category: Category,
    private val tabPosition: Int
) : ViewModel() {
    /** 選択中のIssue */
    var issue : Issue? = null

    /** フィルタされていない全エントリリスト */
    private val entries by lazy {
        MutableLiveData<List<Entry>>()
    }

    /** タブで表示するエントリリスト */
    val filteredEntries by lazy {
        MutableLiveData<List<Entry>>().also { filtered ->
            entries.observeForever {
                viewModelScope.launch {
                    filtered.postValue(repository.filterEntries(it))
                }
            }
        }
    }

    /** フィルタされていない全通知リスト */
    val notices by lazy {
        MutableLiveData<List<Notice>>()
    }

    /** 表示項目リストを初期化 */
    fun refresh(onError: ((Throwable)->Unit)? = null) = viewModelScope.launch {
        when (category) {
            Category.Notices -> refreshNotices(onError)

            else -> refreshEntries(onError)
        }
    }

    /** エントリリストを初期化 */
    private suspend fun refreshEntries(onError: ((Throwable)->Unit)?) = withContext(Dispatchers.Default) {
        try {
            entries.postValue(repository.loadEntries(category, issue, tabPosition))
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }
    }

    /** 通知リストを初期化 */
    private suspend fun refreshNotices(onError: ((Throwable) -> Unit)?) = withContext(Dispatchers.Default) {
        try {
            notices.postValue(repository.loadNotices())
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }
    }

    /** エントリリストの追加ロード */
    fun loadAdditional(
        onFinally: ((Throwable?)->Unit)? = null,
        onError: ((Throwable)->Unit)? = null
    ) = viewModelScope.launch(Dispatchers.Default) {
        var error : Throwable? = null
        try {
            val offset = entries.value?.size ?: 0
            val entries = repository.loadEntries(category, issue, tabPosition, offset)
            val oldItems = this@EntriesTabFragmentViewModel.entries.value ?: emptyList()
            this@EntriesTabFragmentViewModel.entries.postValue(
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
