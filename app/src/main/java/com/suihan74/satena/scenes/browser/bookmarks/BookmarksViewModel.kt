package com.suihan74.satena.scenes.browser.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.utilities.OnFinally
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarksViewModel(
    val repository: BookmarksRepository
) : ViewModel() {

    /** サインイン状態 */
    val signedIn by lazy {
        repository.signedIn
    }

    /** 表示中のページのEntry */
    val entry by lazy {
        repository.entry
    }

    /** 表示中のページのBookmarksEntry */
    val bookmarksEntry by lazy {
        repository.bookmarksEntry
    }

    /** 表示するブクマリスト */
    val bookmarks by lazy {
        repository.recentBookmarks
    }

    // ------ //

    /** 最新ブクマリストを再取得 */
    fun reloadBookmarks(onFinally: OnFinally? = null) = viewModelScope.launch {
        viewModelScope.launch(Dispatchers.Main) {
            runCatching {
                repository.loadRecentBookmarks(
                    additionalLoading = false
                )
            }
            onFinally?.invoke()
        }
    }
}
