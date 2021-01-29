package com.suihan74.satena.scenes.bookmarks

import androidx.lifecycle.*
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks2.BookmarksAdapter
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.RecyclerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksTabViewModel(
    private val repo : BookmarksRepository,

    /** タブに紐づいているブクマリスト */
    private val bookmarks : LiveData<List<Bookmark>>
) : ViewModel() {
    /** 実際に画面に表示するアイテム */
    val displayBookmarks = MutableLiveData<List<RecyclerState<BookmarksAdapter.Entity>>>()

    // ------ //

    fun init(owner: LifecycleOwner) {
        bookmarks.observe(owner, Observer {
            viewModelScope.launch {
                displayBookmarks.postValue(createDisplayBookmarks(it))
            }
        })
    }

    // ------ //

    private suspend fun createDisplayBookmarks(
        bookmarks: List<Bookmark>
    ) : List<RecyclerState<BookmarksAdapter.Entity>> = withContext(Dispatchers.Default) {
        val bookmarksEntry = repo.bookmarksEntry.value ?: return@withContext emptyList()
        val taggedUsers = repo.taggedUsers.mapNotNull { it.value.value }
        val ignoredUsers = repo.ignoredUsersCache
        val displayMutedMention = repo.prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING)

        return@withContext RecyclerState.makeStatesWithFooter(bookmarks.map { bookmark ->
            val analyzedComment = BookmarkCommentDecorator.convert(bookmark.comment)
            BookmarksAdapter.Entity(
                bookmark = bookmark,
                analyzedComment = analyzedComment,
                isIgnored = ignoredUsers.contains(bookmark.user),
                mentions = analyzedComment.ids.mapNotNull { called ->
                    bookmarksEntry.bookmarks
                        .firstOrNull { b -> b.user == called }
                        ?.let { mentioned ->
                            if (!displayMutedMention && ignoredUsers.contains(mentioned.user)) null
                            else mentioned
                        }
                },
                userTags = taggedUsers.firstOrNull { t -> t.user.name == bookmark.user }?.tags
                    ?: emptyList()
            )
        })
    }
}
