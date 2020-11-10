package com.suihan74.satena.scenes.bookmarks.detail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks.repository.StarRelation

class BookmarkDetailViewModel(
    val repository : BookmarksRepository,
    bookmark : Bookmark
) : ViewModel() {

    /** 画面の表示対象のブクマ */
    val bookmark = MutableLiveData<Bookmark>().also {
        it.observeForever { b ->
            ignored.value = repository.checkIgnored(b)
        }
    }

    /**
     * 非表示ユーザーかどうか
     */
    val ignored = MutableLiveData<Boolean>()

    /**
     * 現在選択中の文字列
     *
     * 引用スターに使用
     */
    val selectedText = MutableLiveData<String>()

    /**
     * スター付与メニューの開閉状態
     */
    val starsMenuOpened = MutableLiveData<Boolean>()

    /**
     * ブクマにつけられたスター
     */
    val starsToUser by lazy {
        MutableLiveData<List<StarRelation>>()
    }

    /**
     * ブクマのユーザーがつけたスター
     */
    val starsFromUser by lazy {
        MutableLiveData<List<StarRelation>>()
    }

    /**
     * ブクマに言及している他のブクマ
     */
    val mentionsToUser by lazy {
        MutableLiveData<List<Bookmark>>()
    }

    /**
     * ブクマが言及している他のブクマ
     */
    val mentionsFromUser by lazy {
        MutableLiveData<List<Bookmark>>()
    }

    // ------ //

    init {
        this.bookmark.value = bookmark
    }
}
