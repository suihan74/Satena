package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.Category
import com.suihan74.utilities.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
  TODO: それぞれの画面用に切り分けるべきか
*/

class EntriesTabFragmentViewModel(
    private val repository: EntriesRepository,
    val category: Category,
    private val tabPosition: Int
) : ViewModel() {
    companion object {
        private const val TAB_POSITION_READ_LATER = 1
    }

    /** 選択中のIssue */
    var issue : Issue? = null

    /** 選択中のタグ : Category.MyBookmarks, Category.User */
    var tag : Tag? = null

    /** 表示中のユーザー : Category.User */
    var user : String?
        get() = params.get(LoadEntryParameter.USER)
        set(value) {
            params.put(LoadEntryParameter.USER, value)
        }

    /** 検索クエリ */
    var searchQuery: String?
        get() = params.get(LoadEntryParameter.SEARCH_QUERY)
        set(value) {
            params.put(LoadEntryParameter.SEARCH_QUERY, value)
        }

    /** 検索タイプ */
    var searchType: SearchType
        get() = params.get(LoadEntryParameter.SEARCH_TYPE, SearchType.Text)
        set(value) {
            params.put(LoadEntryParameter.SEARCH_TYPE, value)
        }

    /** 追加パラメータ */
    private val params by lazy { LoadEntryParameter() }

    /** サイトURL : Category.Site */
    var siteUrl: String?
        get() = params.get(LoadEntryParameter.SITE_URL)
        set(value) {
            params.put(LoadEntryParameter.SITE_URL, value)
        }

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

    /** 障害情報 */
    val information by lazy {
        MutableLiveData<List<MaintenanceEntry>>()
    }

    /** フィルタリングを任意で実行する */
    fun filter() {
        entries.value = entries.value
    }

    /** 指定したエントリを削除する */
    fun delete(entry: Entry) {
        entries.value = entries.value?.filterNot { it.id == entry.id }
    }

    /** 指定したエントリのブクマを削除する */
    fun deleteBookmark(entry: Entry) {
        if (category == Category.MyBookmarks) {
            delete(entry)
        }
        else {
            entries.value = entries.value?.map {
                if (it.id == entry.id) it.copy(bookmarkedData = null)
                else it
            }
        }
    }

    /** エントリに付けたブクマを更新する */
    fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult) {
        entries.value = entries.value?.map {
            if (it.id == entry.id) it.copy(bookmarkedData = bookmarkResult)
            else it
        }
    }

    /** 表示項目リストを初期化 */
    fun refresh(onError: ((Throwable)->Unit)? = null) = viewModelScope.launch {
        when (category) {
            Category.Notices -> refreshNotices(onError)
            Category.Maintenance -> refreshInformation(onError)
            else -> refreshEntries(onError)
        }
    }

    /** エントリリストを取得 */
    private suspend fun fetchEntries(offset: Int? = null) : List<Entry> {
        // 追加パラメータを設定
        val params = when (category) {
            Category.Site -> {
                if (siteUrl == null) return emptyList()
                params.also {
                    if (offset != null && offset > 0) {
                        val prev = it.get(LoadEntryParameter.PAGE, 1)
                        it.put(LoadEntryParameter.PAGE, prev + 1)
                    }
                    else {
                        it.put(LoadEntryParameter.PAGE, 1)
                    }
                }
            }

            Category.User,
            Category.MyBookmarks -> {
                when (tabPosition) {
                    TAB_POSITION_READ_LATER ->
                        params.also {
                            it.put(LoadEntryParameter.TAG, "あとで読む")
                        }

                    else ->
                        params.also {
                            it.put(LoadEntryParameter.TAG, tag?.text)
                        }
                }
            }

            Category.Search -> {
                if (searchQuery.isNullOrBlank()) return emptyList()
                else params
            }

            else -> params
        }

        return repository.loadEntries(category, issue, tabPosition, offset, params)
    }

    /** エントリリストを初期化 */
    private suspend fun refreshEntries(onError: ((Throwable)->Unit)?) = withContext(Dispatchers.Default) {
        try {
            entries.postValue(fetchEntries())
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }
    }


    /** 通知リストを初期化 */
    private suspend fun refreshNotices(onError: ((Throwable)->Unit)?) = withContext(Dispatchers.Default) {
        try {
            notices.postValue(repository.loadNotices())
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }
    }

    /** 障害情報を取得 */
    private suspend fun refreshInformation(onError: ((Throwable)->Unit)?) = withContext(Dispatchers.Default) {
        try {
            information.postValue(repository.loadInformation())
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
            val entries = fetchEntries(offset)
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
