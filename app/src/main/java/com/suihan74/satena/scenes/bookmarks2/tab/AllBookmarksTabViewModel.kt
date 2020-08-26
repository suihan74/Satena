package com.suihan74.satena.scenes.bookmarks2.tab

import com.suihan74.satena.models.PreferenceKey

class AllBookmarksTabViewModel : BookmarksTabViewModel() {
    override fun init() {
        super.init()

        val displayIgnored = preferences.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS)
        activityViewModel.bookmarksRecent.observeForever {
            bookmarks.postValue(
                if (displayIgnored) {
                    activityViewModel.keywordFilter(it)
                }
                else {
                    activityViewModel.filter(activityViewModel.keywordFilter(it))
                }
            )
        }
    }

    override fun updateBookmarks() = activityViewModel.updateRecent()
    override fun loadNextBookmarks() = activityViewModel.loadNextRecent()
    override fun updateSignedUserBookmark(user: String) =
        activityViewModel.bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == user }
}
