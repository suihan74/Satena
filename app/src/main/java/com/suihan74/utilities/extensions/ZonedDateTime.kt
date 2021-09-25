package com.suihan74.utilities.extensions

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * `LocalDateTime`をシステムのタイムゾーンの値に変換する
 *
 * @param fromId 変換前の`LocalDateTime`のタイムゾーン
 */
fun LocalDateTime.toSystemZonedDateTime(fromId: String = "Asia/Tokyo") : ZonedDateTime {
    val src = this.atZone(ZoneId.of(fromId))
    return src.withZoneSameInstant(ZoneId.systemDefault())
}

// ------ //

object ZonedDateTimeUtil {
    val MIN = ZonedDateTime.ofLocal(LocalDateTime.MIN, ZoneId.of("UTC"), ZoneOffset.UTC)

    val MAX = ZonedDateTime.ofLocal(LocalDateTime.MAX, ZoneId.of("UTC"), ZoneOffset.UTC)
}
