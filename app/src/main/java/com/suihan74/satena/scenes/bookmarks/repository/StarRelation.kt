package com.suihan74.satena.scenes.bookmarks.repository

import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Star

/**
 * ブクマ詳細画面で表示するリスト項目
 *
 * スターをつけたユーザーとそのユーザーのブクマ
 */
data class StarRelation (
    /** スターをつけたユーザー */
    val sender : String,

    /** スターをつけられたユーザー */
    val receiver : String,

    /** スターをつけたユーザーのブクマ */
    val senderBookmark: Bookmark?,

    /** スターをつけられたブクマ */
    val receiverBookmark : Bookmark,

    /** つけられたスター */
    val star: Star? = null
)
