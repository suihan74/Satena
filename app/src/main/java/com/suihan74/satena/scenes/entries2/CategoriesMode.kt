package com.suihan74.satena.scenes.entries2

import com.suihan74.satena.R

/** カテゴリ一覧の表示方法 */
enum class CategoriesMode(
    val textId: Int
) {
    /** リスト形式 */
    LIST(R.string.pref_categories_mode_list),
    /** テーブル形式 */
    GRID(R.string.pref_categories_mode_grid);

    companion object {
        fun fromInt(int: Int) = values().getOrNull(int) ?: LIST
    }
}
