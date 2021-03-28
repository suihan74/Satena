package com.suihan74.satena.scenes.entries2

import androidx.annotation.StringRes
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.TextIdContainer

/**
 * タブ情報
 */
enum class EntriesTabType(
    /** タブの表示位置 */
    val tabOrdinal : Int,

    /** 表示名 */
    @StringRes override val textId : Int,

    /** タブを使用するカテゴリ */
    val category : Category? = null
) : TextIdContainer {

    HOT(0, R.string.entries_tab_hot),

    RECENT(1, R.string.entries_tab_recent),

    RECENT_STARS(0, R.string.entries_tab_my_stars, Category.Stars),

    STARS_REPORT(1, R.string.entries_tab_stars_report, Category.Stars),

    MY_BOOKMARKS(0,R.string.entries_tab_mybookmarks, Category.MyBookmarks),

    READ_LATER(1, R.string.entries_tab_read_later, Category.MyBookmarks),

    MAINTAIN(EntriesDefaultTabSettings.MAINTAIN, R.string.entries_tab_maintain)
    ;

    companion object {
        fun fromTabOrdinal(pos: Int, category: Category) = values().let { tabs ->
            tabs.firstOrNull { it.tabOrdinal == pos && it.category == category }
                ?: tabs.firstOrNull { it.tabOrdinal == pos && it.category == null }!!
        }

        fun getTabs(category: Category?) : List<EntriesTabType> {
            return values().let { tabs ->
                val items = tabs.filter { it.category == category && it.tabOrdinal >= 0 }
                if (items.isEmpty()) tabs.filter { it.category == null && it.tabOrdinal >= 0 }
                else items
            }
        }
    }
}

// ------ //

/**
 * 各カテゴリの最初に表示するタブ設定
 */
class EntriesDefaultTabSettings : HashMap<Category, Int>() {
    companion object {
        /** 設定しない（遷移時に表示されているタブ位置を維持する） */
        const val MAINTAIN = -1
    }

    // ------ //

    fun getOrDefault(key: Category) : Int = this.getOrElse(key) { MAINTAIN }
}
