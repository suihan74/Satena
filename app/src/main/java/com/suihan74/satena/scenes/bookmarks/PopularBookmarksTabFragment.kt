package com.suihan74.satena.scenes.bookmarks

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Bookmark
import kotlinx.coroutines.launch

/** 人気ブクマリスト表示部分のFragment */
class PopularBookmarksTabFragment : BookmarksTabFragment() {

    companion object {
        fun createInstance() = PopularBookmarksTabFragment()
    }

    // ------ //

    override fun reloadBookmarks() {
        bookmarksViewModel.viewModelScope.launch {
            bookmarksViewModel.repository.loadPopularBookmarks()
        }
    }

    override fun afterLoadedBookmarks() {
    }

    override val bookmarksLiveData: LiveData<List<Bookmark>>
        get() = bookmarksViewModel.popularBookmarks
}
