package com.suihan74.satena.scenes.bookmarks2.tab

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
    override fun loadNextBookmarks() = activityViewModel.loadNextRecent()
    override fun updateSignedUserBookmark(user: String) =
        activityViewModel.bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == user }
}
