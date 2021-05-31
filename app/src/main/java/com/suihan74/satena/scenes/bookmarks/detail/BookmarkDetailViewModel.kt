package com.suihan74.satena.scenes.bookmarks.detail

import androidx.lifecycle.*
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks.repository.StarRelation
import com.suihan74.utilities.exceptions.TaskFailureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class BookmarkDetailViewModel(
    val repository : BookmarksRepository,
    bookmark : Bookmark
) : ViewModel() {

    /** 画面の表示対象のブクマ */
    val bookmark : LiveData<Bookmark> by lazy { _bookmark }

    private val _bookmark = MutableLiveData<Bookmark>().also {
        it.observeForever { b ->
            _ignored.value = repository.checkIgnoredUser(b.user)
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
                _ignored.value = repository.checkIgnoredUser(bookmark.user)
            }
        }
    }

    /**
     * ユーザータグ
     *
     * TODO
     */
    val userTags = runBlocking { repository.getUserTags(bookmark.user) }

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
    val starsToUser = MutableLiveData<List<StarRelation>>()

    /**
     * ブクマのユーザーがつけたスター
     */
    val starsFromUser = MutableLiveData<List<StarRelation>>()

    /**
     * ブクマに言及している他のブクマ
     */
    val mentionsToUser = MutableLiveData<List<StarRelation>>()

    /**
     * ブクマが言及している他のブクマ
     */
    val mentionsFromUser = MutableLiveData<List<StarRelation>>()

    // ------ //

    init {
        this._bookmark.value = bookmark
    }

    fun onCreateView(owner: LifecycleOwner) {
        repository.recentBookmarks.observe(owner, Observer {
            viewModelScope.launch {
                reload()
            }
        })
    }

    // ------ //

    private suspend fun reload() {
        runCatching {
            updateList(DetailTabAdapter.TabType.STARS_TO_USER, true)
            updateList(DetailTabAdapter.TabType.STARS_FROM_USER, true)
            updateList(DetailTabAdapter.TabType.MENTION_TO_USER, true)
            updateList(DetailTabAdapter.TabType.MENTION_FROM_USER, true)
        }
    }

    /** タブに対応するリストを取得する */
    fun getList(tabType: DetailTabAdapter.TabType) : LiveData<List<StarRelation>> {
        return when (tabType) {
            DetailTabAdapter.TabType.STARS_TO_USER -> starsToUser

            DetailTabAdapter.TabType.STARS_FROM_USER -> starsFromUser

            DetailTabAdapter.TabType.MENTION_TO_USER -> mentionsToUser

            DetailTabAdapter.TabType.MENTION_FROM_USER -> mentionsFromUser
        }
    }

    /**
     * タブに対応するリストを更新する
     *
     * @throws TaskFailureException
     */
    suspend fun updateList(
        tabType: DetailTabAdapter.TabType,
        forceUpdate: Boolean = false
    ) = withContext(Dispatchers.Default) {
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

            DetailTabAdapter.TabType.MENTION_TO_USER -> {
                if (forceUpdate || mentionsToUser.value == null) {
                    val bookmarks = repository.getMentionsTo(bookmark)
                    val relations = bookmarks.map {
                        StarRelation(
                            sender = it.user,
                            receiver = bookmark.user,
                            senderBookmark = it,
                            receiverBookmark = bookmark
                        )
                    }
                    mentionsToUser.postValue(relations)
                }
            }

            DetailTabAdapter.TabType.MENTION_FROM_USER -> {
                if (forceUpdate || mentionsFromUser.value == null) {
                    val bookmarks = repository.getMentionsFrom(bookmark)
                    val relations = bookmarks.map {
                        StarRelation(
                            sender = bookmark.user,
                            receiver = it.user,
                            senderBookmark = bookmark,
                            receiverBookmark = it
                        )
                    }
                    mentionsFromUser.postValue(relations)
                }
            }
        }
    }
}
