package com.suihan74.satena.models.browser

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDateTime

// ページ情報をアドレス入力時のサジェスチョンなどに使用することを想定して冗長にしている

/**
 * ページ情報
 *
 * 何回訪れたか、最後にいつ訪れたかなどの情報
 */
@Entity(
    tableName = "browser_history_pages"
)
data class HistoryPage (
    val url : String,

    val title : String,

    val faviconUrl : String,

    val lastVisited : LocalDateTime,

    val visitTimes : Long = 1L,

    @PrimaryKey(autoGenerate = true)
    val id : Long = 0L
)

/**
 * 閲覧履歴
 *
 * 訪れた時点とどのページを訪れたかという情報
 */
@Entity(
    tableName = "browser_history_items",
)
data class HistoryLog (
    val visitedAt : LocalDateTime,

    val pageId : Long = 0L,

    @PrimaryKey(autoGenerate = true)
    val id : Long = 0L
)

/**
 * アプリ側で閲覧履歴リストに表示するための情報
 */
data class History (
    @Embedded
    val log : HistoryLog,

    @Relation(
        parentColumn = "pageId",
        entityColumn = "id"
    )
    val page : HistoryPage
)
