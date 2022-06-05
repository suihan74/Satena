package com.suihan74.satena.scenes.entries2

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.*
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.EntrySearchSetting
import com.suihan74.satena.models.ExtraScrollingAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
  TODO: それぞれの画面用に切り分けるべきか
*/

class EntriesTabFragmentViewModel(
    private val repository: EntriesRepository,
    readEntriesRepository: ReadEntriesRepository,
    val category: Category,
    private val tabPosition: Int
) :
    ViewModel(),
    EntryMenuActions by EntryMenuActionsImplForEntries(repository, SatenaApplication.instance.readEntriesRepository),
    CommentMenuActions by CommentMenuActionsImpl(repository)
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

    /** 検索パラメータ */
    var temporarySearchSetting: LiveData<EntrySearchSetting?>? = null

    /** 追加パラメータ */
    private val params by lazy { LoadEntryParameter() }

    /** サイトURL : Category.Site */
    var siteUrl: String?
        get() = params.get(LoadEntryParameter.SITE_URL)
        set(value) {
            params.put(LoadEntryParameter.SITE_URL, value)
        }

    /** フィルタされていない全エントリリスト */
    private val entries = MutableLiveData<List<Entry>>().apply {
        observeForever {
            viewModelScope.launch(Dispatchers.Default) {
                filter(it.orEmpty())
            }
        }
    }

    /** タブで表示するエントリリスト */
    val filteredEntries : LiveData<List<Entry>> = MutableLiveData()
    private val _filteredEntries = filteredEntries as MutableLiveData<List<Entry>>

    /** フィルタによって除外されたエントリ */
    val excludedEntries : LiveData<List<Entry>> = MutableLiveData()
    private val _excludedEntries = excludedEntries as MutableLiveData<List<Entry>>

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

    /** ロード済みの既読エントリID */
    val readEntryIds = repository.readEntryIds

    val extraScrollingAlignment
        get() = repository.extraScrollingAlignment

    val extraScrollBarVisibility =
        MutableLiveData(extraScrollingAlignment != ExtraScrollingAlignment.NONE)

    // ------ //

    init {
        combine(readEntriesRepository.readEntryBehavior, readEntriesRepository.categoriesHidingReadEntries, ::Pair)
            .onEach { filter() }
            .launchIn(viewModelScope)
    }

    // ------ //

    fun onResume() {
        extraScrollBarVisibility.value = extraScrollingAlignment != ExtraScrollingAlignment.NONE
    }

    // ------ //

    /** フィルタリングを任意で実行する */
    suspend fun filter() {
        filter(entries.value.orEmpty())
    }

    private suspend fun filter(entries: List<Entry>) = withContext(Dispatchers.Default) {
        val (activeEntries, inactiveEntries) = repository.filterEntries(category, entries)
        _filteredEntries.postValue(activeEntries)
        _excludedEntries.postValue(inactiveEntries)
    }

    /** 指定したエントリを削除する */
    suspend fun delete(entry: Entry) = withContext(Dispatchers.Default) {
        entries.postValue(
            entries.value?.filterNot { it.same(entry) }
        )
    }

    /** 指定したエントリのブクマを削除する */
    suspend fun deleteBookmark(entry: Entry) = withContext(Dispatchers.Default) {
        if (category == Category.MyBookmarks) {
            delete(entry)
        }
        else {
            entries.postValue(
                entries.value?.map {
                    if (it.same(entry)) it.copy(bookmarkedData = null)
                    else it
                }
            )
        }
    }

    /** エントリに付けたブクマを更新する */
    suspend fun updateBookmark(
        entry: Entry,
        bookmarkResult: BookmarkResult?
    ) = withContext(Dispatchers.Default) {
        entries.postValue(
            entries.value?.map {
                if (it.same(entry)) it.copy(bookmarkedData = bookmarkResult)
                else it
            }
        )
    }

    /** 表示項目リストを初期化 */
    suspend fun reloadLists() {
        when (category) {
            Category.Notices -> loadNotices()
            Category.Maintenance -> loadInformation()
            else -> loadEntries()
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
                else params.also {
                    it.put(LoadEntryParameter.TEMPORARY_SEARCH_SETTING, temporarySearchSetting?.value)
                }
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
    suspend fun loadAdditional() = withContext(Dispatchers.Default) {
        val offset = entries.value?.size ?: 0
        val entries = fetchEntries(offset)
        val oldItems = this@EntriesTabFragmentViewModel.entries.value ?: emptyList()
        this@EntriesTabFragmentViewModel.entries.postValue(
            oldItems.plus(entries).distinctBy { it.url }
        )
    }

    // ------ //

    /** 通知を削除する */
    suspend fun deleteNotice(notice: Notice) {
        repository.deleteNotice(notice)
        reloadLists()
    }

    // ------ //

    /** エントリをシングルクリックしたときの処理 */
    fun onClickEntry(
        activity: FragmentActivity,
        entry: Entry,
        fragmentManager: FragmentManager
    ) = super.invokeEntryClickedAction(
        activity,
        entry,
        repository.entryClickedAction,
        fragmentManager
    )

    /** エントリを複数回クリックしたときの処理 */
    fun onMultipleClickEntry(
        activity: FragmentActivity,
        entry: Entry,
        fragmentManager: FragmentManager
    ) = super.invokeEntryClickedAction(
        activity,
        entry,
        repository.entryMultipleClickedAction,
        fragmentManager
    )

    /** エントリを長押ししたときの処理 */
    fun onLongClickEntry(
        activity: FragmentActivity,
        entry: Entry,
        fragmentManager: FragmentManager
    ) = super.invokeEntryClickedAction(
        activity,
        entry,
        repository.entryLongClickedAction,
        fragmentManager
    )

    /** エントリ右端をシングルクリックしたときの処理 */
    fun onClickEntryEdge(
        activity: FragmentActivity,
        entry: Entry,
        fragmentManager: FragmentManager
    ) = super.invokeEntryClickedAction(
        activity,
        entry,
        repository.entryEdgeClickedAction,
        fragmentManager
    )

    /** エントリ右端を複数回クリックしたときの処理 */
    fun onMultipleClickEntryEdge(
        activity: FragmentActivity,
        entry: Entry,
        fragmentManager: FragmentManager
    ) = super.invokeEntryClickedAction(
        activity,
        entry,
        repository.entryEdgeMultipleClickedAction,
        fragmentManager
    )

    /** エントリ右端を長押ししたときの処理 */
    fun onLongClickEntryEdge(
        activity: FragmentActivity,
        entry: Entry,
        fragmentManager: FragmentManager
    ) = super.invokeEntryClickedAction(
        activity,
        entry,
        repository.entryEdgeLongClickedAction,
        fragmentManager
    )

    /** コメント部分クリック時の処理 */
    fun onClickComment(
        activity: FragmentActivity,
        entry: Entry,
        bookmarkResult: BookmarkResult
    ) {
        openComment(activity, entry, bookmarkResult)
    }

    /** コメント部分長押し時の処理 */
    fun onLongClickComment(
        entry: Entry,
        bookmarkResult: BookmarkResult,
        fragmentManager: FragmentManager
    ) {
        openCommentMenuDialog(entry, bookmarkResult, fragmentManager)
    }
}
