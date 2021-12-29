package com.suihan74.satena.scenes.bookmarks.detail.tabs

import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.detail.DetailTabAdapter
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarkMenuActionsImpl
import com.suihan74.utilities.extensions.putObjectExtra
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StarRelationsTabViewModel(
    val tabType : DetailTabAdapter.TabType,
    val repository : BookmarksRepository,
    private val targetEntry: Entry,
    private val targetBookmark : LiveData<Bookmark>
) : ViewModel() {

    private val bookmarkMenuActions = BookmarkMenuActionsImpl(repository)

    private var entry : Entry? = null
    private val entryLoadingMutex = Mutex()

    // ------ //

    init {
        viewModelScope.launch {
            entryLoadingMutex.withLock {
                entry = when (tabType) {
                    DetailTabAdapter.TabType.BOOKMARKS_TO_USER ->
                        targetBookmark.value?.getCommentPageUrl(targetEntry)?.let { url ->
                            repository.getEntry(url)
                        }
                    else -> targetEntry
                }
            }
        }
    }

    // ------ //

    /**
     * スター項目をクリックしたときの挙動
     *
     * コメントがあるならその詳細ページを開く
     */
    fun onClickItem(activity: BookmarksActivity, item: StarRelationsAdapter.Item) {
        when (tabType) {
            DetailTabAdapter.TabType.BOOKMARKS_TO_USER -> runCatching {
                val intent = Intent(activity, BookmarksActivity::class.java).also {
                    it.putObjectExtra(BookmarksActivity.EXTRA_ENTRY, entry!!)
                    it.putExtra(BookmarksActivity.EXTRA_TARGET_USER, item.user)
                }
                activity.startActivity(intent)
            }

            else -> {
                val bookmark = item.bookmark
                if (bookmark == null) {
                    val user = item.user
                    activity.contentsViewModel.openEmptyBookmarkDetail(
                        activity,
                        entry!!,
                        user
                    )
                }
                else {
                    activity.contentsViewModel.openBookmarkDetail(
                        activity,
                        entry!!,
                        bookmark
                    )
                }
            }
        }
    }

    /**
     * スター項目に対するメニューを開く
     */
    suspend fun openStarRelationMenuDialog(
        item: StarRelationsAdapter.Item,
        fragmentManager: FragmentManager,
    ) {
        val bookmark = item.bookmark ?: Bookmark(user = item.user, comment = "")
        val userSignedIn = repository.userSignedIn
        val entry = entryLoadingMutex.withLock { this.entry ?: return }

        // 詳細表示中のブクマに自分がつけたスターを取り消すため，
        // 自分の項目では詳細表示中ブクマのスターを取得する
        val isMyBookmark = tabType == DetailTabAdapter.TabType.STARS_TO_USER && userSignedIn == bookmark.user
        val starsEntry = runCatching {
            if (isMyBookmark) repository.getStarsEntry(item.relation.receiverBookmark)
            else repository.getStarsEntry(bookmark)
        }.getOrNull()

        val starDeletingTarget =
            if (isMyBookmark) item.relation.receiverBookmark
            else null

        bookmarkMenuActions.openBookmarkMenuDialog(
            entry,
            bookmark,
            starsEntry?.value,
            fragmentManager,
            starDeletingTarget
        )
    }
}
