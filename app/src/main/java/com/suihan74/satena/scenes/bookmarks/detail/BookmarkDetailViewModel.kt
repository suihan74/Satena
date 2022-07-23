package com.suihan74.satena.scenes.bookmarks.detail

import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks.repository.StarRelation
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarkMenuActionsImpl
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.utilities.exceptions.TaskFailureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class BookmarkDetailViewModel(
    val repository : BookmarksRepository,
    val entry : Entry,
    bookmark : Bookmark
) : ViewModel() {

    private val _bookmark = MutableLiveData<Bookmark>().also {
        it.observeForever { b ->
            viewModelScope.launch(Dispatchers.Default) {
                _ignored.postValue(repository.isIgnored(b.user))
                repository.loadUserTags(b.user)
            }
        }
    }

    /** 画面の表示対象のブクマ */
    val bookmark : LiveData<Bookmark> = _bookmark

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
                viewModelScope.launch(Dispatchers.Default) {
                    _ignored.postValue(repository.isIgnored(bookmark.user))
                }
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
     * ブクマにつけられたブコメ
     */
    val bookmarksToUser = MutableLiveData<List<StarRelation>>()

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
            updateList(DetailTabAdapter.TabType.BOOKMARKS_TO_USER)
            updateList(DetailTabAdapter.TabType.STARS_TO_USER, true)
            updateList(DetailTabAdapter.TabType.STARS_FROM_USER, true)
            updateList(DetailTabAdapter.TabType.MENTION_TO_USER, true)
            updateList(DetailTabAdapter.TabType.MENTION_FROM_USER, true)
        }
    }

    /** タブに対応するリストを取得する */
    fun getList(tabType: DetailTabAdapter.TabType) : LiveData<List<StarRelation>> {
        return when (tabType) {
            DetailTabAdapter.TabType.BOOKMARKS_TO_USER -> bookmarksToUser

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
            DetailTabAdapter.TabType.BOOKMARKS_TO_USER -> {
                if (forceUpdate || bookmarksToUser.value == null) {
                    val bookmarks = repository.getBookmarksToBookmark(bookmark)
                    val relations = bookmarks.map {
                        StarRelation(
                            sender = it.user,
                            receiver = bookmark.user,
                            senderBookmark = it,
                            receiverBookmark = bookmark
                        )
                    }
                    bookmarksToUser.postValue(relations)
                }
            }

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

    suspend fun updateBookmarksToUser() {
        runCatching {
            bookmark.value?.let { repository.getBookmarkCounts(it) }
            updateList(DetailTabAdapter.TabType.BOOKMARKS_TO_USER, forceUpdate = true)
        }
    }

    /** ブクマをブクマするためのアクティビティを開く */
    suspend fun openPostBookmarkActivity(fragment: BookmarkDetailFragment) = withContext(Dispatchers.Main) {
        starsMenuOpened.value = false
        val intent = Intent(fragment.requireContext(), BookmarkPostActivity::class.java).also {
            it.putExtra(
                BookmarkPostActivity.EXTRA_URL,
                bookmark.value!!.getCommentPageUrl(repository.entry.value!!)
            )
        }
        fragment.bookmarkPostActivityLauncher.launch(intent)
    }

    // ------ //

    private val bookmarkMenuActions by lazy { BookmarkMenuActionsImpl(repository) }

    /** ブクマ項目に対する操作メニューを表示 */
    suspend fun openBookmarkMenuDialog(fragmentManager: FragmentManager) {
        val bookmark = bookmark.value!!
        val starsEntry = runCatching { repository.getStarsEntry(bookmark) }.getOrNull()
        bookmarkMenuActions.openBookmarkMenuDialog(
            entry,
            bookmark,
            starsEntry?.value,
            fragmentManager
        )
    }
}
