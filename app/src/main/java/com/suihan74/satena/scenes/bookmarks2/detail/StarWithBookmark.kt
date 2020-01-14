package com.suihan74.satena.scenes.bookmarks2.detail

import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.Star

data class StarWithBookmark (
    /** スター情報 */
    val star: Star?,
    /** 該当ブクマが存在しない場合，ダミーを作成すること */
    val bookmark: Bookmark,
    /** 表示指定 */
    val state: DisplayState
) {
    enum class DisplayState {
        /** 表示 */
        SHOW,
        /** 非表示対象であることを明記して表示する */
        COVER
    }

    override fun equals(other: Any?): Boolean {
        if (other !is StarWithBookmark) return false

        return other.star?.user == star?.user &&
               other.star?.color == star?.color &&
               other.star?.count == star?.count &&
               other.star?.quote == star?.quote &&
               other.bookmark.comment == bookmark.comment &&
               other.state == state
    }

    override fun hashCode(): Int {
        var result = star?.hashCode() ?: 0
        result = 31 * result + bookmark.hashCode()
        result = 31 * result + state.hashCode()
        return result
    }
}
