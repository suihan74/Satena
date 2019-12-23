package com.suihan74.satena.scenes.bookmarks2.tab

import kotlinx.coroutines.Job

class PopularTabViewModel : BookmarksTabViewModel() {
    override fun init() {
        super.init()
        bookmarksViewModel.bookmarksPopular.observeForever {
            bookmarks.postValue(
                bookmarksViewModel.filter(it)
            )
        }
    }

    override fun updateBookmarks() = bookmarksViewModel.updateDigest()
    override fun loadNextBookmarks() = Job()
}
