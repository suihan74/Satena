package com.suihan74.satena.scenes.bookmarks2.tab

class RecentTabViewModel : BookmarksTabViewModel() {
    override fun init() {
        super.init()
        bookmarksViewModel.bookmarksRecent.observeForever {
            bookmarks.postValue(
                bookmarksViewModel.filter(
                    it.filter { b -> b.comment.isNotBlank() }
                )
            )
        }
    }

    override fun updateBookmarks() = bookmarksViewModel.updateRecent()
    override fun loadNextBookmarks() = bookmarksViewModel.loadNextRecent()
    override fun updateSignedUserBookmark(user: String) =
        bookmarksViewModel.bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == user }
}
