package com.suihan74.satena.models.browser

import com.suihan74.satena.R
import com.suihan74.satena.models.TextIdContainer

/**
 * アプリ内ブラウザ閲覧履歴の寿命（日数）
 */
enum class BrowserHistoryLifeSpan(
    val days: Int,
    override val textId: Int
) : TextIdContainer {

    WEEK_1(7, R.string.browser_history_lifespan_1_week),

    WEEK_2(14, R.string.browser_history_lifespan_2_week),

    WEEK_3(21, R.string.browser_history_lifespan_3_week),

    MONTH_1(30, R.string.browser_history_lifespan_1_month),

    MONTH_3(90, R.string.browser_history_lifespan_3_month),

    YEAR_0_5(180, R.string.browser_history_lifespan_half_year),

    YEAR_1(365, R.string.browser_history_lifespan_1_year),

    NO_LIMIT(0, R.string.browser_history_lifespan_no_limit)
    ;

    companion object {
        fun fromDays(days: Int) = values().firstOrNull { it.days == days } ?: WEEK_3
    }
}
