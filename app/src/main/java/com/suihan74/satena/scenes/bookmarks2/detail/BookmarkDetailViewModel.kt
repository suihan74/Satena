package com.suihan74.satena.scenes.bookmarks2.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.scenes.bookmarks2.BookmarksRepository
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class BookmarkDetailViewModel(
    val bookmarksRepository: BookmarksRepository,
    val bookmark: Bookmark
) : ViewModel() {

    val starsToUser = bookmarksRepository.createStarsEntryLiveData(bookmark)
    val starsFromUser
        get() =
            bookmarksRepository.run {
                getStarsEntryFrom(bookmark.user).mapNotNull { starsEntry ->
                    bookmarksEntry?.bookmarks?.firstOrNull { b ->
                        b.getBookmarkUrl(entry) == starsEntry.url
                    }
                }
            }

    fun updateStarsToUser(onError: CompletionHandler? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e -> onError?.invoke(e) }
    ) {
        starsToUser.updateAsync().await()
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
