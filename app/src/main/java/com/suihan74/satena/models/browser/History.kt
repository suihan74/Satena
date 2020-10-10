package com.suihan74.satena.models.browser

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.threeten.bp.LocalDateTime

/**
 * 内部ブラウザ閲覧履歴
 */
@Entity(
    tableName = "history",
    indices = [
        Index(value = ["url"], name = "url", unique = true)
    ]
)
data class History (
    @PrimaryKey
    val url : String,

    val title : String,

    val faviconUrl : String,

    val lastVisited : LocalDateTime,

    val visitTimes : Long = 1L
)
