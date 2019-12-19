package com.suihan74.satena.scenes.bookmarks2.tab

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import kotlinx.coroutines.Job


abstract class BookmarksTabViewModel : ViewModel() {
    lateinit var bookmarksViewModel: BookmarksViewModel

    val bookmarks by lazy {
        MutableLiveData<List<Bookmark>>()
    }

    abstract fun init()
    abstract fun updateBookmarks() : Job
    abstract fun loadNextBookmarks() : Job

    class Factory (
        private val bookmarksViewModel: BookmarksViewModel
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            (modelClass.newInstance() as BookmarksTabViewModel).apply {
                bookmarksViewModel = this@Factory.bookmarksViewModel
            } as T
    }
}


class PopularTabViewModel : BookmarksTabViewModel() {
    override fun init() {
        bookmarksViewModel.bookmarksPopular.observeForever {
            bookmarks.postValue(it)
        }
    }

    override fun updateBookmarks() = bookmarksViewModel.updateDigest()
    override fun loadNextBookmarks() = Job()
}

class RecentTabViewModel : BookmarksTabViewModel() {
    override fun init() {
        bookmarksViewModel.bookmarksRecent.observeForever {
            bookmarks.postValue(it.filter { b -> b.comment.isNotBlank() })
        }
    }

    override fun updateBookmarks() = bookmarksViewModel.updateRecent()
    override fun loadNextBookmarks() = bookmarksViewModel.loadNextRecent()
}

class AllBookmarksTabViewModel : BookmarksTabViewModel() {
    override fun init() {
        bookmarksViewModel.bookmarksRecent.observeForever {
            bookmarks.postValue(it)
        }
    }

    override fun updateBookmarks() = bookmarksViewModel.updateRecent()
    override fun loadNextBookmarks() = bookmarksViewModel.loadNextRecent()
}
