package com.suihan74.satena.scenes.entries2

import android.content.res.ColorStateList
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.MenuItemCompat
import com.suihan74.satena.R

/** ボトムバーに表示する項目 */
enum class UserBottomItem(
    val id: Int,
    @DrawableRes val iconId: Int,
    @StringRes val textId: Int,
    val requireSignedIn: Boolean = false,
    val longClickable: Boolean = false
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

    INNER_BROWSER(9,
        R.drawable.ic_world,
        R.string.inner_browser,
        longClickable = true
    ),

    SEARCH(3,
        R.drawable.ic_baseline_search,
        R.string.category_search
    ),

    PREFERENCES(4,
        R.drawable.ic_baseline_settings,
        R.string.preferences_desc
    ),

    OPEN_OFFICIAL_TOP(5,
        R.drawable.ic_category_social,
        R.string.open_official_top_desc
    ),

    OPEN_OFFICIAL_HATENA(6,
        R.drawable.ic_category_social,
        R.string.open_official_hatena_desc
    ),

    OPEN_ANONYMOUS_DIARY(7,
        R.drawable.ic_category_social,
        R.string.open_anonymous_diary_desc
    ),

    HOME(7,
        R.drawable.ic_category_all,
        R.string.home_category_desc
    ),

    CATEGORIES(8,
        R.drawable.ic_baseline_category,
        R.string.categories_desc
    ),

    ;

    /** Menuに追加する */
    fun toMenuItem(menu: Menu, tint: ColorStateList) : MenuItem =
        menu.add(textId).apply {
            setIcon(iconId)
            MenuItemCompat.setIconTintList(this, tint)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
}
