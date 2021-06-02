package com.suihan74.satena.scenes.entries2

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.Category
import com.suihan74.utilities.OnError
import com.suihan74.utilities.OnFinally
import com.suihan74.utilities.OnSuccess
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
) :
    ViewModel(),
    EntryMenuActions by EntryMenuActionsImplForEntries(repository)
{
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
                    filtered.postValue(repository.filterEntries(it ?: emptyList()))
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

    /** ユーザーの歴史を取得する : Category.Memorial15th */
    var isUserMemorial : Boolean = false

    /** フィルタリングを任意で実行する */
    fun filter() {
        entries.value = entries.value
    }

    /** 指定したエントリを削除する */
    fun delete(entry: Entry) {
        entries.value = entries.value?.filterNot { it.same(entry) }
    }

    /** 指定したエントリのブクマを削除する */
    fun deleteBookmark(entry: Entry) {
        if (category == Category.MyBookmarks) {
            delete(entry)
        }
        else {
            entries.value = entries.value?.map {
                if (it.same(entry)) it.copy(bookmarkedData = null)
                else it
            }
        }
    }

    /** エントリに付けたブクマを更新する */
    fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult) {
        entries.value = entries.value?.map {
            if (it.same(entry)) it.copy(bookmarkedData = bookmarkResult)
            else it
        }
    }

    /** 表示項目リストを初期化 */
    fun reloadLists(
        onSuccess: OnSuccess<Unit>? = null,
        onError: OnError? = null,
        onFinally: OnFinally? = null
    ) = viewModelScope.launch {
        try {
            when (category) {
                Category.Notices -> loadNotices()
                Category.Maintenance -> loadInformation()
                else -> loadEntries()
            }

            onSuccess?.invoke(Unit)
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }
        finally {
            withContext(Dispatchers.Main) {
                onFinally?.invoke()
            }
        }
    }

    /** エントリリストを取得 */
    private suspend fun fetchEntries(offset: Int? = null) : List<Entry> {
        // 追加パラメータを設定
        val params = when (category) {
            Category.Site,
            Category.FavoriteSites -> {
                if (category == Category.Site && siteUrl == null) return emptyList()
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

            Category.Memorial15th -> params.also {
                it.put(LoadEntryParameter.IS_USER, isUserMemorial)
            }

            else -> params
        }

        return repository.loadEntries(category, issue, tabPosition, offset, params)
    }

    /** エントリリストを初期化 */
    private suspend fun loadEntries() = withContext(Dispatchers.Default) {
        try {
            entries.postValue(fetchEntries())
        }
        catch (e: Throwable) {
            throw e
        }
    }

    /** 通知リストを初期化 */
    private suspend fun loadNotices() = withContext(Dispatchers.Default) {
        try {
            notices.postValue(repository.loadNotices())
        }
        catch (e: Throwable) {
            throw e
        }
    }

    /** 障害情報を取得 */
    private suspend fun loadInformation() = withContext(Dispatchers.Default) {
        try {
            information.postValue(repository.loadInformation())
        }
        catch (e: Throwable) {
            throw e
        }
    }

    /** エントリリストの追加ロード */
    fun loadAdditional(
        onFinally: OnFinally? = null,
        onError: OnError? = null
    ) = viewModelScope.launch(Dispatchers.Default) {
        try {
            val offset = entries.value?.size ?: 0
            val entries = fetchEntries(offset)
            val oldItems = this@EntriesTabFragmentViewModel.entries.value ?: emptyList()
            this@EntriesTabFragmentViewModel.entries.postValue(
                oldItems.plus(entries).distinctBy { it.url }
            )
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }
        finally {
            withContext(Dispatchers.Main) {
                onFinally?.invoke()
            }
        }
    }

    // ------ //

    /** 通知を削除する */
    fun deleteNotice(notice: Notice, onError: OnError? = null) {
        runCatching {
            repository.deleteNotice(notice)
        }
        reloadLists(onError = onError)
    }

    // ------ //

    /** エントリをシングルクリックしたときの処理 */
    fun onClickEntry(
        activity: FragmentActivity,
        entry: Entry,
        fragmentManager: FragmentManager
    ) {
        super.invokeEntryClickedAction(
            activity,
            entry,
            repository.entryClickedAction,
            fragmentManager,
            viewModelScope
        )
    }

    /** エントリを複数回クリックしたときの処理 */
    fun onMultipleClickEntry(
        activity: FragmentActivity,
        entry: Entry,
        fragmentManager: FragmentManager
    ) {
        super.invokeEntryClickedAction(
            activity,
            entry,
            repository.entryMultipleClickedAction,
            fragmentManager,
            viewModelScope
        )
    }

    /** エントリを長押ししたときの処理 */
    fun onLongClickEntry(
        activity: FragmentActivity,
        entry: Entry,
        fragmentManager: FragmentManager
    ) {
        super.invokeEntryClickedAction(
            activity,
            entry,
            repository.entryLongClickedAction,
            fragmentManager,
            viewModelScope
        )
    }
}
