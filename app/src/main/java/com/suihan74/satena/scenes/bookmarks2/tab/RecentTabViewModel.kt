package com.suihan74.satena.scenes.bookmarks2.tab

import com.suihan74.hatenaLib.Bookmark

class RecentTabViewModel : BookmarksTabViewModel() {
    override fun init() {
        super.init()
        activityViewModel.bookmarksRecent.observeForever {
            bookmarks.postValue(
                activityViewModel.filter(
                    it.filter { b -> b.comment.isNotBlank() }
                )
            )
        }
    }

    override fun updateBookmarks() = activityViewModel.updateRecent()

    override suspend fun loadNextBookmarks() =
        try {
            activityViewModel.loadNextRecent().map { Bookmark.create(it) }
        }
        catch (e: Throwable) {
            emptyList()
        }

    override fun updateSignedUserBookmark(user: String) =
        activityViewModel.bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == user }
}
