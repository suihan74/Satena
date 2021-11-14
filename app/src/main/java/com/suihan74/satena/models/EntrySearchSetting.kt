package com.suihan74.satena.models

import com.suihan74.hatenaLib.SearchType
import java.time.LocalDate

/**
 * エントリ検索パラメータ
 */
data class EntrySearchSetting(
    /** 検索対象 */
    val searchType: SearchType = SearchType.Title,
    /** 最小ブクマ数 */
    val users : Int = 1,
    /** 対象期間(開始) */
    val dateBegin : LocalDate? = null,
    /** 対象期間(終了) */
    val dateEnd : LocalDate? = null,
    /** セーフサーチ */
    val safe : Boolean = false
)
