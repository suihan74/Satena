package com.suihan74.satena.scenes.bookmarks.detail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository

class BookmarkDetailViewModel(
    val repository : BookmarksRepository,
    bookmark : Bookmark
) : ViewModel() {

    /** 画面の表示対象のブクマ */
    val bookmark = MutableLiveData<Bookmark>(bookmark)

    /**
     * 現在選択中の文字列
     *
     * 引用スターに使用
     */
    val selectedText = MutableLiveData<String>()
}
