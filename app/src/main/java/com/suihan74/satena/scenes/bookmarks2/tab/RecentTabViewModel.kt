package com.suihan74.satena.scenes.bookmarks2.tab

import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.utilities.SafeSharedPreferences

class RecentTabViewModel(
    activityViewModel: BookmarksViewModel,
    prefs: SafeSharedPreferences<PreferenceKey>
) : BookmarksTabViewModel(activityViewModel, prefs) {
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
            activityViewModel.loadNextRecent()
        }
        catch (e: Throwable) {
            emptyList()
        }

    override fun updateSignedUserBookmark(user: String) =
        activityViewModel.bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == user }
}
