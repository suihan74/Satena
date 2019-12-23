package com.suihan74.satena.scenes.bookmarks2.tab

class CustomTabViewModel : BookmarksTabViewModel() {
    override fun init() {
        super.init()
        bookmarksViewModel.bookmarksRecent.observeForever {
            bookmarks.postValue(
                bookmarksViewModel.keywordFilter(it)
            )
        }
    }

    override fun updateBookmarks() = bookmarksViewModel.updateRecent()
    override fun loadNextBookmarks() = bookmarksViewModel.loadNextRecent()
    override fun updateSignedUserBookmark(user: String) =
        bookmarksViewModel.bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == user }
}
