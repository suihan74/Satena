package com.suihan74.satena.scenes.bookmarks2.tab

import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.utilities.SafeSharedPreferences

class PopularTabViewModel(
    activityViewModel: BookmarksViewModel,
    prefs: SafeSharedPreferences<PreferenceKey>
) : BookmarksTabViewModel(activityViewModel, prefs) {
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
