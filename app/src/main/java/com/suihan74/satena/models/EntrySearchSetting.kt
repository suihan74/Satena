package com.suihan74.satena.models

import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.R
import java.time.LocalDate

/**
 * エントリ検索パラメータ
 */
data class EntrySearchSetting(
    /** 検索対象 */
    val searchType: SearchType = SearchType.Title,
    /** 最小ブクマ数 */
    val users : Int = 1,
    /** 期間指定の方法 */
    val dateMode: EntrySearchDateMode = EntrySearchDateMode.RECENT,
    /** 対象期間(開始) */
    val dateBegin : LocalDate? = null,
    /** 対象期間(終了) */
    val dateEnd : LocalDate? = null,
    /** セーフサーチ */
    val safe : Boolean = false
)

// ------ //

/** 期間指定の方法 */
enum class EntrySearchDateMode(
    override val textId: Int
) : TextIdContainer {
    /** 直近N日間 */
    RECENT(R.string.entry_search_date_mode_recent),
    /** カレンダーで指定 */
    CALENDAR(R.string.entry_search_date_mode_calendar);
}

val EntrySearchDateMode?.orDefault
    get() = this ?: EntrySearchDateMode.RECENT
