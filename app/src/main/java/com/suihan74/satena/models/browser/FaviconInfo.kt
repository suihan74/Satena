package com.suihan74.satena.models.browser

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

@Entity(
    tableName = "browser_favicon_info",
    indices = [
        Index(value = ["domain"], name = "favicon_info_domain", unique = true)
    ]
)
data class FaviconInfo(
    val domain : String,

    val filename : String,

    val lastUpdated : ZonedDateTime,

    @PrimaryKey(autoGenerate = true)
    val id : Long = 0
)
