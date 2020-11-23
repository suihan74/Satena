package com.suihan74.satena.scenes.bookmarks.detail.tabs

import android.app.Activity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.detail.DetailTabAdapter
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarkMenuActionsImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    fun openStarRelationMenuDialog(
        activity: Activity,
        item: StarRelationsAdapter.Item,
        fragmentManager: FragmentManager,
        coroutineScope: CoroutineScope
    ) = coroutineScope.launch(Dispatchers.Main) {
        val bookmark = item.bookmark ?: return@launch
        val userSignedIn = repository.userSignedIn
        val entry = repository.entry.value ?: return@launch

        // 詳細表示中のブクマに自分がつけたスターを取り消すため，
        // 自分の項目では詳細表示中ブクマのスターを取得する
        val starsEntry =
            if (tabType == DetailTabAdapter.TabType.STARS_TO_USER && userSignedIn == bookmark.user) {
                repository.getStarsEntry(item.relation.receiverBookmark)
            }
            else repository.getStarsEntry(bookmark)

        bookmarkMenuActions.openBookmarkMenuDialog(
            activity,
            entry,
            bookmark,
            starsEntry?.value,
            fragmentManager,
            coroutineScope
        )
    }
}
