package com.suihan74.satena.scenes.entries2

import com.suihan74.satena.R

/** ボトムバーに表示する項目 */
enum class UserBottomItem(
    val id: Int,
    val iconId: Int,
    val textId: Int
) {
    SCROLL_TO_TOP(0,
        R.drawable.ic_vertical_align_top,
        R.string.scroll_to_top
    ),

    NOTICE(1,
        R.drawable.ic_notifications,
        R.string.notices_desc
    ),

    MYBOOKMARKS(2,
        R.drawable.ic_mybookmarks,
        R.string.my_bookmarks_desc
    ),

    PREFERENCES(3,
        R.drawable.ic_baseline_settings,
        R.string.preferences_desc
    ),

    // TODO: カテゴリ項目で置き換える
    CATEGORY(4,
        R.drawable.ic_baseline_category,
        R.string.categories_desc
    ),
}
