package com.suihan74.satena.scenes.bookmarks2.detail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.scenes.bookmarks2.BookmarksRepository
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class BookmarkDetailViewModel(
    /** BookmarksActivityのViewModel */
    val bookmarksRepository: BookmarksRepository,
    /** 表示対象のブックマーク */
    val bookmark: Bookmark
) : ViewModel() {

    /** 詳細画面で表示中のブクマについたスターを監視 */
    val starsToUser = bookmarksRepository.createStarsEntryLiveData(bookmark)

    /** エントリに関する全スター情報を監視 */
    val starsAll = bookmarksRepository.allStarsLiveData

    /** サインインしているユーザーの所持スター情報 */
    val userStars = bookmarksRepository.userStarsLiveData

    /** スターメニューが開いているか */
    val starsMenuOpened by lazy { MutableLiveData<Boolean>() }

    /** 初期化 */
    fun init(onError: CompletionHandler? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        starsMenuOpened.postValue(false)
        userStars.load()
    }

    /** userに付いたスターと，それを付けた人の同記事へのブクマを取得する */
    fun getStarsWithBookmarkTo(user: String) : List<StarWithBookmark> {
        val bookmarks = bookmarksRepository.bookmarksEntry?.bookmarks ?: emptyList()
        val recentBookmarks = bookmarksRepository.bookmarksRecent

        return bookmarksRepository.getStarsEntryTo(user)?.allStars?.map {
            val bookmark =
                recentBookmarks.firstOrNull { b -> b.user == it.user }
                ?: bookmarks.firstOrNull { b -> b.user == it.user }
                ?: Bookmark(user = it.user, comment = "")

            StarWithBookmark(it, bookmark)
        } ?: emptyList()
    }

    /** userが付けたスターと，そのスターがついたブクマを取得する */
    fun getStarsWithBookmarkFrom(user: String) : List<StarWithBookmark> {
        val bookmarks = bookmarksRepository.bookmarksEntry?.bookmarks ?: emptyList()
        val recentBookmarks = bookmarksRepository.bookmarksRecent

        return bookmarksRepository.getStarsEntryFrom(user).mapNotNull m@ {
            val bookmark =
                recentBookmarks.firstOrNull { b -> it.url.contains("/${b.user}/") }
                ?: bookmarks.firstOrNull { b -> it.url.contains("/${b.user}/") }
                ?: return@m null

            val star = it.allStars.firstOrNull { s -> s.user == user }
                ?: return@m null

            StarWithBookmark(star, bookmark)
        }
    }

    fun updateStarsToUser(onError: CompletionHandler? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        starsToUser.updateAsync().await()
    }

    fun updateStarsAll(forceUpdate: Boolean, onError: CompletionHandler? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        starsAll.updateAsync(forceUpdate).await()
    }

    class Factory(
        private val bookmarksRepository: BookmarksRepository,
        private val bookmark: Bookmark
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            BookmarkDetailViewModel(bookmarksRepository, bookmark) as T
    }
}
