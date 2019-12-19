package com.suihan74.satena.scenes.bookmarks2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.BookmarksEntry
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch


class BookmarksViewModel(
    val repository: BookmarksRepository
) : ViewModel() {

    val entry
        get() = repository.entry

    val bookmarksEntry by lazy {
        MutableLiveData<BookmarksEntry>()
    }

    val bookmarksPopular by lazy {
        MutableLiveData<List<Bookmark>>()
    }

    val bookmarksRecent by lazy {
        MutableLiveData<List<Bookmark>>()
    }


    /** 初期化 */
    fun load(onError: ((Throwable)->Unit)? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e -> onError?.invoke(e)}
    ) {
        listOf(
            repository.loadIgnoredUsersAsync(),
            repository.loadBookmarksEntryAsnyc(),
            repository.loadBookmarksDigestAsync(),
            repository.loadBookmarksRecentAsync()
        ).run {
            awaitAll()
            bookmarksEntry.postValue(repository.bookmarksEntry)
            bookmarksPopular.postValue(
                filter(repository.bookmarksPopular)
            )
            bookmarksRecent.postValue(
                filter(repository.bookmarksRecent)
            )
        }
    }

    /** 非表示ユーザー情報を適用したブクマリストを返す */
    fun filter(bookmarks: List<Bookmark>) : List<Bookmark> =
        bookmarks.filterNot { b -> repository.ignoredUsers.any { it == b.user } }

    /** 新着ブクマリストの次のページを追加ロードする */
    fun loadNextRecent(onError: ((Throwable)->Unit)? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e -> onError?.invoke(e)}
    ) {
        repository.loadNextBookmarksRecentAsync().await()
        bookmarksRecent.postValue(
            filter(repository.bookmarksRecent)
        )
    }

    /** 人気ブクマリストを再読み込み */
    fun updateDigest(onError: ((Throwable)->Unit)? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e -> onError?.invoke(e)}
    ) {
        repository.loadBookmarksDigestAsync().await()
        bookmarksPopular.postValue(
            filter(repository.bookmarksPopular)
        )
    }

    /** 新着ブクマリストを再読み込み */
    fun updateRecent(onError: ((Throwable)->Unit)? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e -> onError?.invoke(e)}
    ) {
        repository.loadBookmarksRecentAsync().await()
        bookmarksRecent.postValue(
            filter(repository.bookmarksRecent)
        )
    }

    /** ViewModelProvidersを使用する際の依存性注入 */
    class Factory(private val repository: BookmarksRepository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            BookmarksViewModel(repository) as T
    }
}
