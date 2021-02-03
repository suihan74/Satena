package com.suihan74.satena.scenes.bookmarks

import androidx.fragment.app.FragmentManager

/**
 * ブクマ詳細画面を表示できる画面
 */
interface BookmarkDetailOpenable {
    val fragmentManager : FragmentManager

    /** Fragmentで置き換えるビューのID */
    val bookmarkDetailFrameLayoutId : Int
}
