package com.suihan74.satena.scenes.entries2

import androidx.annotation.StringRes
import com.suihan74.satena.R
import com.suihan74.satena.models.TextIdContainer

/** カテゴリ一覧の表示方法 */
enum class CategoriesMode(
    @StringRes override val textId: Int
) : TextIdContainer {
    /** リスト形式 */
    LIST(R.string.pref_categories_mode_list),
    /** グリッド形式 */
    GRID(R.string.pref_categories_mode_grid);

    companion object {
        fun fromOrdinal(int: Int) = values().getOrElse(int) { LIST }
    }
}
