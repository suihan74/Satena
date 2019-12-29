package com.suihan74.satena.scenes.bookmarks2.tab

import com.suihan74.satena.models.PreferenceKey

class AllBookmarksTabViewModel : BookmarksTabViewModel() {
    override fun init() {
        super.init()

        val displayIgnored = preferences.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS)
        bookmarksViewModel.bookmarksRecent.observeForever {
            bookmarks.postValue(
                if (displayIgnored) {
                    bookmarksViewModel.keywordFilter(it)
                }
                else {
                    bookmarksViewModel.filter(bookmarksViewModel.keywordFilter(it))
                }
            )
        }
    }

    override fun updateBookmarks() = bookmarksViewModel.updateRecent()
    override fun loadNextBookmarks() = bookmarksViewModel.loadNextRecent()
    override fun updateSignedUserBookmark(user: String) =
        bookmarksViewModel.bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == user }
}
