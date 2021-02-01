package com.suihan74.satena.scenes.bookmarks.viewModel

import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.utilities.AnalyzedBookmarkComment
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.RecyclerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksTabViewModel(
    private val repo : BookmarksRepository,

    /** タブに紐づいているブクマリスト */
    bookmarks : LiveData<List<Bookmark>>
) : ViewModel() {
    /** 実際に画面に表示するアイテム */
    val displayBookmarks = MutableLiveData<List<RecyclerState<Entity>>>()

    // ------ //

    init {
        bookmarks.observeForever {
            viewModelScope.launch {
                displayBookmarks.postValue(createDisplayBookmarks(it))
            }
        }
    }

    // ------ //

    private suspend fun createDisplayBookmarks(
        bookmarks: List<Bookmark>
    ) : List<RecyclerState<Entity>> = withContext(Dispatchers.Default) {
        val bookmarksEntry = repo.bookmarksEntry.value
        val taggedUsers = repo.taggedUsers.mapNotNull { it.value.value }
        val ignoredUsers = repo.ignoredUsersCache
        val displayMutedMention = repo.prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING)

        return@withContext RecyclerState.makeStatesWithFooter(bookmarks.map { bookmark ->
            val analyzedComment = BookmarkCommentDecorator.convert(bookmark.comment)
            Entity(
                bookmark = bookmark,
                analyzedComment = analyzedComment,
                isIgnored = ignoredUsers.contains(bookmark.user),
                mentions = analyzedComment.ids.mapNotNull { called ->
                    bookmarksEntry?.bookmarks
                        ?.firstOrNull { b -> b.user == called }
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

// ------ //

data class Entity (
    val bookmark: Bookmark,
    val analyzedComment: AnalyzedBookmarkComment,
    val isIgnored: Boolean,
    val mentions: List<Bookmark>,
    val userTags: List<Tag>
) {
    override fun equals(other: Any?): Boolean {
        if (other !is Entity) return false

        return bookmark.same(other.bookmark) &&
                isIgnored == other.isIgnored &&
                mentions.contentsEquals(other.mentions) { a, b -> a.same(b) } &&
                userTags.contentsEquals(other.userTags) { a, b -> a.name == b.name }
    }

    override fun hashCode() = super.hashCode()

    // ------ //

    class DiffCallback : DiffUtil.ItemCallback<RecyclerState<Entity>>() {
        override fun areItemsTheSame(
            oldItem: RecyclerState<Entity>,
            newItem: RecyclerState<Entity>
        ): Boolean {
            return oldItem.type == newItem.type && oldItem.body?.bookmark?.user == newItem.body?.bookmark?.user
        }

        override fun areContentsTheSame(
            oldItem: RecyclerState<Entity>,
            newItem: RecyclerState<Entity>
        ): Boolean {
            return oldItem.type == newItem.type && oldItem.body?.equals(newItem.body) == true
        }
    }

    // ------ //

    private fun <T> List<T>?.contentsEquals(other: List<T>?, comparator: ((T, T)->Boolean) = { a, b -> a == b }) =
        if (this == null && other == null) true
        else if (this == null && other != null) false
        else if (this != null && other == null) false
        else this!!.size == other!!.size && this.mapIndexed { index, _ -> comparator(this[index], other[index]) }.all { it }
}
