package com.suihan74.satena.scenes.bookmarks2.detail

import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.Star

/** スター情報にそれを付けたユーザーのコメントを付与 */
data class StarWithBookmark (
    val star: Star?,
    /** 該当ブクマが存在しない場合，ダミーを作成すること */
    val bookmark: Bookmark
)
