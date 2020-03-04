package com.suihan74.hatenaLib

import org.threeten.bp.LocalDateTime
import java.io.Serializable

data class MaintenanceEntry (
    val id: String,
    val title: String,
    val body: String,
    val resolved: Boolean,
    val url: String,
    val timestamp: LocalDateTime,
    val timestampUpdated: LocalDateTime
) : Serializable
