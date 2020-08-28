package com.suihan74.satena.scenes.entries2

import android.content.res.ColorStateList
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.MenuItemCompat
import com.suihan74.satena.R

/** ボトムバーに表示する項目 */
enum class UserBottomItem(
    val id: Int,
    val iconId: Int,
    val textId: Int,
    val requireSignedIn: Boolean = false
) {
    SCROLL_TO_TOP(0,
        R.drawable.ic_vertical_align_top,
        R.string.scroll_to_top
    ),

    NOTICE(1,
        R.drawable.ic_notifications,
        R.string.notices_desc,
        requireSignedIn = true
    ),

    MYBOOKMARKS(2,
        R.drawable.ic_mybookmarks,
        R.string.my_bookmarks_desc,
        requireSignedIn = true
    ),

    PREFERENCES(3,
        R.drawable.ic_baseline_settings,
        R.string.preferences_desc
    ),

    HOME(4,
        R.drawable.ic_category_all,
        R.string.home_category_desc
    ),

    // TODO: カテゴリ項目で置き換える
    CATEGORY(5,
        R.drawable.ic_baseline_category,
        R.string.categories_desc
    );

    /** Menuに追加する */
    fun toMenuItem(menu: Menu, tint: ColorStateList) : MenuItem =
        menu.add(textId).apply {
            setIcon(iconId)
            MenuItemCompat.setIconTintList(this, tint)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
}
