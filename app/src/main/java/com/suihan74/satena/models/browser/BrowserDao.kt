package com.suihan74.satena.models.browser

import androidx.room.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

@Dao
interface BrowserDao {

    // --- history --- //

    @Transaction
    @Query("""
        select * from browser_history_items
        order by visitedAt desc
        limit :offset, :limit
        """)
    suspend fun getRecentHistories(offset: Int = 0, limit: Int = 10) : List<History>

    @Query("""
        select * from browser_history_pages
        where url=:url
        limit 1
        """)
    suspend fun getHistoryPage(url: String): HistoryPage?

    @Transaction
    @Query("""
        select * from browser_history_items
        where visitedAt = :visited
        limit 1
    """)
    suspend fun getHistory(visited: LocalDateTime) : History?

    @Transaction
    @Query("""
        select * from browser_history_items
        where visitedAt>=:start and visitedAt<:end and pageId = :pageId
        """)
    suspend fun findHistory(pageId: Long, start: LocalDateTime, end: LocalDateTime) : List<HistoryLog>

    // ------ //
    // INSERT

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryPage(page: HistoryPage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryLog)

    @Transaction
    suspend fun insertHistory(history: History) {
        insertHistoryPage(history.page)
        val page = getHistoryPage(history.page.url)!!

        // 同日内の同一URL履歴を削除する
        val date = history.log.visitedAt.toLocalDate()
        val startOfDay = LocalTime.ofSecondOfDay(0L)
        val start = LocalDateTime.of(date, startOfDay)
        val end = LocalDateTime.of(date.plusDays(1L), startOfDay)
        findHistory(page.id, start, end).forEach {
            deleteHistoryItem(it)
        }

        val item = history.log.copy(pageId = page.id)
        insertHistoryItem(item)
    }

    // ------ //
    // DELETE

    @Transaction
    suspend fun deleteHistoryPage(page: HistoryPage) {
        deleteHistoryItemsWithPage(page.id)
        __deleteHistoryPageImpl(page)
    }

    @Delete
    suspend fun __deleteHistoryPageImpl(page: HistoryPage)

    @Delete
    suspend fun deleteHistoryItem(item: HistoryLog)

    @Query("""
        delete from browser_history_items
        where pageId = :pageId
    """)
    suspend fun deleteHistoryItemsWithPage(pageId: Long)

    @Query("""
        delete from browser_history_items
        where visitedAt>=:start and visitedAt<:end
    """)
    suspend fun deleteHistory(start: LocalDateTime, end: LocalDateTime)

    // ------ //
    // CLEAR

    @Query("delete from browser_history_items")
    suspend fun clearHistoryItems()

    @Query("delete from browser_history_pages")
    suspend fun clearHistoryPages()

    @Transaction
    suspend fun clearHistory() {
        clearHistoryItems()
        clearHistoryPages()
    }

    // --------------- //
}
