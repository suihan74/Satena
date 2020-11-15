package com.suihan74.satena.scenes.bookmarks.detail

import android.app.Activity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.detail.tabs.StarRelationsAdapter
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks.repository.StarRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarkDetailViewModel(
    val repository : BookmarksRepository,
    bookmark : Bookmark
) : ViewModel() {

    /** 画面の表示対象のブクマ */
    val bookmark : LiveData<Bookmark> by lazy { _bookmark }

    private val _bookmark = MutableLiveData<Bookmark>().also {
        it.observeForever { b ->
            _ignored.value = repository.checkIgnored(b)
            viewModelScope.launch {
                repository.loadUserTags(b.user)
            }
        }
    }

    /**
     * 非表示ユーザーかどうか
     */
    val ignored : LiveData<Boolean> by lazy { _ignored }

    private val _ignored = MutableLiveData<Boolean>()

    /**
     * 非表示ユーザーのリスト
     */
    val ignoredUsers by lazy {
        repository.ignoredUsers.also {
            it.observeForever {
                _ignored.value = repository.checkIgnored(bookmark)
            }
        }
    }

    /**
     * ユーザータグ
     */
    val userTags = repository.getUserTags(bookmark.user)

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
        this._bookmark.value = bookmark
    }

    // ------ //

    /** タブに対応するリストを取得する */
    fun getList(tabType: DetailTabAdapter.TabType) : LiveData<List<StarRelation>> {
        viewModelScope.launch(Dispatchers.Default) {
            updateList(tabType, forceUpdate = false)
        }

        return when (tabType) {
            DetailTabAdapter.TabType.STARS_TO_USER -> starsToUser

            DetailTabAdapter.TabType.STARS_FROM_USER -> starsFromUser

            // TODO
            else -> throw NotImplementedError()
        }
    }

    /** タブに対応するリストを更新する */
    suspend fun updateList(tabType: DetailTabAdapter.TabType, forceUpdate: Boolean) {
        val bookmark = bookmark.value!!
        when (tabType) {
            DetailTabAdapter.TabType.STARS_TO_USER -> {
                if (forceUpdate || starsToUser.value == null) {
                    starsToUser.postValue(
                        repository.getStarRelationsTo(bookmark, forceUpdate)
                    )
                }
            }

            DetailTabAdapter.TabType.STARS_FROM_USER -> {
                if (forceUpdate || starsFromUser.value == null) {
                    starsFromUser.postValue(
                        repository.getStarRelationsFrom(bookmark, forceUpdate)
                    )
                }
            }

            // TODO
            else -> throw NotImplementedError()
        }
    }

    // ------ //

    /**
     * スター項目をクリックしたときの挙動
     *
     * コメントがあるならその詳細ページを開く
     */
    fun onClickStarRelation(activity: BookmarksActivity, item: StarRelationsAdapter.Item?) {
        if (item == null) return

        val bookmark = item.bookmark
        if (bookmark == null) {
            val user = item.user
            activity.contentsViewModel.openEmptyBookmarkDetail(
                activity,
                user
            )
        }
        else {
            activity.contentsViewModel.openBookmarkDetail(
                activity,
                bookmark
            )
        }
    }

    /**
     * スター項目に対するメニューを開く
     */
    fun openStarRelationMenuDialog(
        activity: Activity,
        user: String,
        bookmark: Bookmark?,
        fragmentManager: FragmentManager
    ) {
    }
}
