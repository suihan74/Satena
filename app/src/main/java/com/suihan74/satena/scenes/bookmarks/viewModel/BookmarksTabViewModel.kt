package com.suihan74.satena.scenes.bookmarks.viewModel

import androidx.annotation.MainThread
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.scenes.bookmarks.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.utilities.AnalyzedBookmarkComment
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import com.suihan74.utilities.extensions.onNotEmpty
import com.suihan74.utilities.extensions.parallelMap
import kotlinx.coroutines.*

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
    fun setBookmarksLiveData(owner: LifecycleOwner, liveData: LiveData<List<Bookmark>>?, tabType: BookmarksTabType) {
        bookmarks?.removeObservers(owner)
        repo.bookmarksDigest.removeObservers(owner)
        when (tabType) {
            BookmarksTabType.POPULAR -> setPopularBookmarksLiveData(owner)
            else -> setRecentBookmarksLiveData(owner, liveData!!, tabType)
        }
        _bookmarksTabType.value = tabType
    }

    /**
     * 「注目」タブの表示内容をセットする
     */
    @MainThread
    private fun setPopularBookmarksLiveData(owner: LifecycleOwner) {
        repo.bookmarksDigest.observe(owner, Observer {
            viewModelScope.launch(Dispatchers.Main) {
                displayBookmarks.value = createDisplayBookmarksDigest()
            }
        })
    }

    @MainThread
    private fun setRecentBookmarksLiveData(owner: LifecycleOwner, liveData: LiveData<List<Bookmark>>, tabType: BookmarksTabType) {
        bookmarks = liveData.also { ld ->
            ld.observe(owner, Observer { rawList ->
                viewModelScope.launch(Dispatchers.Main) {
                    displayBookmarks.value = rawList?.let {
                        createDisplayBookmarks(it).plus(RecyclerState(type = RecyclerType.FOOTER))
                    } ?: emptyList()
                }
            })
        }
    }

    // ------ //

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun createDisplayBookmarks(
        bookmarks: List<Bookmark>
    ) : List<RecyclerState<Entity>> = withContext(Dispatchers.Default) {
        val taggedUsers = repo.withTaggedUsers { taggedUsers ->
            taggedUsers.mapNotNull {
                it.value.value
            }
        }
        val ignoredUsers = repo.ignoredUsers.value.orEmpty()

        return@withContext bookmarks.parallelMap { bookmark ->
            val analyzedComment = BookmarkCommentDecorator.convert(bookmark.comment)
            val starCounts =
                bookmark.starCount?.let { stars ->
                    if (stars.isEmpty()) null
                    else hashMapOf(
                        StarColor.Yellow to 0,
                        StarColor.Red to 0,
                        StarColor.Green to 0,
                        StarColor.Blue to 0,
                        StarColor.Purple to 0
                    ).also { starCounts ->
                        stars.forEach {
                            starCounts[it.color] = starCounts[it.color]?.plus(it.count) ?: it.count
                        }
                    }
                }

            RecyclerState(
                type = RecyclerType.BODY,
                body = Entity(
                    bookmark = bookmark,
                    analyzedComment = analyzedComment,
                    isIgnored = ignoredUsers.contains(bookmark.user),
                    mentions = repo.getMentionsFrom(bookmark, analyzedComment),
                    userTags = taggedUsers.firstOrNull { t -> t.user.name == bookmark.user }?.tags ?: emptyList(),
                    starCounts = starCounts,
                    repo.getBookmarkCounts(bookmark)
                )
            )
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun createDisplayBookmarksDigest() : List<RecyclerState<Entity>> = withContext(Dispatchers.Default) {
        buildList {

            createDisplayBookmarks(repo.followingsBookmarks()).onNotEmpty { followingsBookmarks ->
                add(RecyclerState(type = RecyclerType.SECTION, extra = R.string.bookmarks_digest_section_followings))
                addAll(followingsBookmarks)
            }

            createDisplayBookmarks(repo.popularBookmarks()).onNotEmpty { popularBookmarks ->
                add(RecyclerState(type = RecyclerType.SECTION, extra = R.string.bookmarks_digest_section_scored))
                addAll(popularBookmarks)
            }

            add(RecyclerState(type = RecyclerType.FOOTER))
            Unit
        }
    }
}

// ------ //

data class Entity (
    val bookmark: Bookmark,
    val analyzedComment: AnalyzedBookmarkComment,
    val isIgnored: Boolean,
    val mentions: List<Bookmark>,
    val userTags: List<Tag>,
    val starCounts: Map<StarColor, Int>?,
    val bookmarkCount: LiveData<Int>
) {
    override fun equals(other: Any?): Boolean {
        if (other !is Entity) return false

        return bookmark.same(other.bookmark) &&
                isIgnored == other.isIgnored &&
                containsNGWords == other.containsNGWords &&
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
