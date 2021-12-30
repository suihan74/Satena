package com.suihan74.satena.models.readEntry

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

/**
 * 既読エントリ情報
 *
 * エントリIDがついているもの（誰かが既にブクマしているもの）に限定
 */
@Entity(tableName = "read_entry")
data class ReadEntry(
    @PrimaryKey(autoGenerate = false)
    val eid : Long,

    val timestamp: ZonedDateTime
)
