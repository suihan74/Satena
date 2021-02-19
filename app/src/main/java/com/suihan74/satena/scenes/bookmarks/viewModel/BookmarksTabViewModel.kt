package com.suihan74.satena.scenes.bookmarks.viewModel

import androidx.annotation.MainThread
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.scenes.bookmarks.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.utilities.AnalyzedBookmarkComment
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.RecyclerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksTabViewModel(
    private val repo : BookmarksRepository,
) : ViewModel() {
    var bookmarks : LiveData<List<Bookmark>>? = null
        private set

    private val _bookmarksTabType = MutableLiveData<BookmarksTabType>()
    val bookmarksTabType : LiveData<BookmarksTabType> = _bookmarksTabType

    /** 実際に画面に表示するアイテム */
    val displayBookmarks = MutableLiveData<List<RecyclerState<Entity>>>()

    // ------ //

    /** 表示対象のブクマリストを変更する */
    @MainThread
    fun setBookmarksLiveData(owner: LifecycleOwner, liveData: LiveData<List<Bookmark>>, tabType: BookmarksTabType) {
        bookmarks?.removeObservers(owner)
        bookmarks = liveData.also {
            it.observe(owner, Observer {
                viewModelScope.launch {
                    displayBookmarks.postValue(createDisplayBookmarks(it))
                }
            })
        }
        _bookmarksTabType.value = tabType
    }

    // ------ //

    private suspend fun createDisplayBookmarks(
        bookmarks: List<Bookmark>
    ) : List<RecyclerState<Entity>> = withContext(Dispatchers.Default) {
        val taggedUsers = repo.withTaggedUsers { taggedUsers ->
            taggedUsers.mapNotNull {
                it.value.value
            }
        }
        val ignoredUsers = repo.ignoredUsersCache

        return@withContext RecyclerState.makeStatesWithFooter(bookmarks.map { bookmark ->
            val analyzedComment = BookmarkCommentDecorator.convert(bookmark.comment)
            Entity(
                bookmark = bookmark,
                analyzedComment = analyzedComment,
                isIgnored = ignoredUsers.contains(bookmark.user),
                mentions = repo.getMentionsFrom(bookmark, analyzedComment),
                userTags = taggedUsers.firstOrNull { t -> t.user.name == bookmark.user }?.tags ?: emptyList()
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
