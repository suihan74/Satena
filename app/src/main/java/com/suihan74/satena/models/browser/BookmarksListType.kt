package com.suihan74.satena.models.browser

import androidx.annotation.StringRes
import com.suihan74.satena.R
import com.suihan74.satena.models.TextIdContainer
import com.suihan74.satena.scenes.bookmarks.BookmarksTabType

/**
 * ブラウザドロワのブクマリスト種類
 *
 * デフォルトで表示するものを選択する用途で使用
 */
enum class BookmarksListType(
    @StringRes override val textId : Int,
    val bookmarksTabType : BookmarksTabType?
) : TextIdContainer {
    POPULAR(R.string.bookmarks_tab_popular, BookmarksTabType.POPULAR),

    RECENT(R.string.bookmarks_tab_recent, BookmarksTabType.RECENT),

    ALL(R.string.bookmarks_tab_all, BookmarksTabType.ALL),

    CUSTOM(R.string.bookmarks_tab_custom, BookmarksTabType.CUSTOM),

    SAME_AS_BOOKMARKS(R.string.browser_bookmarks_list_same_as_bookmarks, null)
    ;

    companion object {
        fun fromOrdinal(position: Int) = values()[position]
    }
}
