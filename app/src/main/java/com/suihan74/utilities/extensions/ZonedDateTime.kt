package com.suihan74.utilities.extensions

import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * `LocalDateTime`をシステムのタイムゾーンの値に変換する
 *
 * @param fromId 変換前の`LocalDateTime`のタイムゾーン
 */
fun LocalDateTime.toSystemZonedDateTime(fromId: String = "Asia/Tokyo") : ZonedDateTime {
    val src = this.atZone(ZoneId.of(fromId))
    return src.withZoneSameInstant(ZoneId.systemDefault())
}
