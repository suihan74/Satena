package com.suihan74.hatenaLib

import org.threeten.bp.LocalDateTime

data class MaintenanceEntry (
    val id: String,
    val title: String,
    val body: String,
    val resolved: Boolean,
    val url: String,
    val timestamp: LocalDateTime,
    val timestampUpdated: LocalDateTime
) {
    // for Gson
    private constructor() : this("", "", "", false, "", LocalDateTime.MIN, LocalDateTime.MIN)
}
