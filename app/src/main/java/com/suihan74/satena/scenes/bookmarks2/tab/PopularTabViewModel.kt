package com.suihan74.satena.scenes.bookmarks2.tab

import com.suihan74.hatenaLib.Bookmark

class PopularTabViewModel : BookmarksTabViewModel() {
    override fun init() {
        super.init()
        activityViewModel.bookmarksPopular.observeForever {
            bookmarks.postValue(
                activityViewModel.filter(it)
            )
        }
    }

    override fun updateBookmarks() = activityViewModel.updateDigest()
    override suspend fun loadNextBookmarks() = emptyList<Bookmark>()
}
