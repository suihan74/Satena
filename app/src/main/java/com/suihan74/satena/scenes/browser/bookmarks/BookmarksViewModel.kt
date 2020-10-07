package com.suihan74.satena.scenes.browser.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.utilities.Listener
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

    /** ブクマ投稿後に呼ばれるイベント */
    private var afterPostedListener : Listener<BookmarkResult>? = null

    /** ブクマ投稿後に呼ばれるイベントをセットする */
    fun setAfterPostedListener(l: Listener<BookmarkResult>?) {
        afterPostedListener = l
    }

    // ------ //

    /** 最新ブクマリストを再取得 */
    fun reloadBookmarks(onFinally: OnFinally? = null) = viewModelScope.launch {
        viewModelScope.launch(Dispatchers.Main) {
            repository.loadRecentBookmarks(
                additionalLoading = false
            )
            onFinally?.invoke()
        }
    }
}
