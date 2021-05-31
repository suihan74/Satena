package com.suihan74.satena.scenes.bookmarks.detail.tabs

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.detail.DetailTabAdapter
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarkMenuActionsImpl

class StarRelationsTabViewModel(
    val tabType: DetailTabAdapter.TabType,
    val repository: BookmarksRepository
) : ViewModel() {

    private val bookmarkMenuActions = BookmarkMenuActionsImpl(repository)

    /**
     * スター項目をクリックしたときの挙動
     *
     * コメントがあるならその詳細ページを開く
     */
    fun onClickItem(activity: BookmarksActivity, item: StarRelationsAdapter.Item) {
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
    suspend fun openStarRelationMenuDialog(
        item: StarRelationsAdapter.Item,
        fragmentManager: FragmentManager,
    ) {
        val bookmark = item.bookmark ?: Bookmark(user = item.user, comment = "")
        val userSignedIn = repository.userSignedIn
        val entry = repository.entry.value ?: return

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
