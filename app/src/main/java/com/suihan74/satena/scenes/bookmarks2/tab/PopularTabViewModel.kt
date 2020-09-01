package com.suihan74.satena.scenes.bookmarks2.tab

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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
    override fun loadNextBookmarks() = viewModelScope.launch {}
}
