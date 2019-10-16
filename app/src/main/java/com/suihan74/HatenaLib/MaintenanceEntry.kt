package com.suihan74.HatenaLib

import org.threeten.bp.LocalDateTime
import java.io.Serializable

data class MaintenanceEntry (
    val id: String,
    val title: String,
    val body: String,
    val url: String,
    val timestamp: LocalDateTime,
    val timestampUpdated: LocalDateTime
) : Serializable
