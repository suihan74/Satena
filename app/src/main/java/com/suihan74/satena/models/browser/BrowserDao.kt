package com.suihan74.satena.models.browser

import androidx.room.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

@Dao
interface BrowserDao {

    // --- history --- //

    /**
     * 直近の閲覧履歴を指定件数取得する
     */
    @Transaction
    @Query("""
        select * from browser_history_items
        order by visitedAt desc
        limit :offset, :limit
        """)
    suspend fun getRecentHistories(offset: Int = 0, limit: Int = 10) : List<History>

    /**
     * 指定URLのページ情報を取得する
     */
    @Query("""
        select * from browser_history_pages
        where url=:url
        limit 1
        """)
    suspend fun getHistoryPage(url: String): HistoryPage?

    /**
     * 最後に訪れたのが指定期間のページ情報を取得する
     */
    @Query("""
        select * from browser_history_pages
        where lastVisited>=:start and lastVisited<:end
    """)
    suspend fun findHistoryPages(start: LocalDateTime, end: LocalDateTime) : List<HistoryPage>

    /**
     * 指定日時の閲覧履歴を取得する
     *
     * 完全に一致する時刻を指定する必要があるため、
     * 主に履歴追加直後にIDが付加されたインスタンスを再取得するために使用する
     */
    @Transaction
    @Query("""
        select * from browser_history_items
        where visitedAt = :visited
        limit 1
    """)
    suspend fun getHistory(visited: LocalDateTime) : History?

    /**
     * 指定期間内で指定ページを参照している閲覧履歴を取得する
     */
    @Query("""
        select * from browser_history_items
        where visitedAt>=:start and visitedAt<:end and pageId = :pageId
        """)
    suspend fun findHistory(pageId: Long, start: LocalDateTime, end: LocalDateTime) : List<HistoryLog>

    /**
     * 履歴を検索する
     */
    @Transaction
    @Query("""
        select * from browser_history_items
        where pageId in (
            select id from browser_history_pages
            where title like :query or url like :query
        )
        order by visitedAt desc
        limit :offset, :limit
    """)
    suspend fun __findHistory(query: String, offset: Int, limit: Int) : List<History>

    /**
     * 履歴を検索する
     */
    @Transaction
    suspend fun findHistory(query: String = "", offset: Int = 0, limit: Int = 10) : List<History> {
        return if (query.isBlank()) {
            getRecentHistories(offset, limit)
        }
        else {
            val modifiedQuery =
                Regex("""[%_]""").let { r ->
                    "%${query.replace(r) { m -> "\\" + m.value }}%"
                }
            __findHistory(modifiedQuery, offset, limit)
        }
    }

    // ------ //
    // INSERT

    /**
     * ページ情報を追加する
     *
     * 外部から直接使用しない
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun __insertHistoryPage(page: HistoryPage)

    /**
     * ログを追加する
     *
     * 外部から直接使用しない
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun __insertHistoryLog(item: HistoryLog)

    /**
     * 閲覧履歴を追加する
     *
     * ページ情報を追加or更新した後にそのページIDを追加したログを追加する
     * 外部からはこのメソッドを使用して項目追加する
     */
    @Transaction
    suspend fun insertHistory(history: History) {
        __insertHistoryPage(history.page)
        val page = getHistoryPage(history.page.url)!!

        // 同日内の同一URL履歴を削除する
        val date = history.log.visitedAt.toLocalDate()
        val startOfDay = LocalTime.ofSecondOfDay(0L)
        val start = LocalDateTime.of(date, startOfDay)
        val end = LocalDateTime.of(date.plusDays(1L), startOfDay)
        findHistory(page.id, start, end).forEach {
            deleteHistoryLog(it)
        }

        val item = history.log.copy(pageId = page.id)
        __insertHistoryLog(item)
    }

    // ------ //
    // DELETE

    /**
     * ページ情報を削除する
     *
     * 加えて、該当のページ情報を参照するすべてのログを削除する
     */
    @Transaction
    suspend fun deleteHistoryPage(page: HistoryPage) {
        deleteHistoryLogsWithPage(page.id)
        __deleteHistoryPageImpl(page)
    }

    /**
     * ページ情報を削除する
     *
     * 外部からは直接使用しない
     */
    @Delete
    suspend fun __deleteHistoryPageImpl(page: HistoryPage)

    /**
     * 最終訪問日時が指定した期間内の全てのページ情報（とそれを参照する閲覧履歴）を削除する
     */
    @Transaction
    suspend fun deleteHistoryPages(start: LocalDateTime, end: LocalDateTime) {
        findHistoryPages(start, end).forEach { page ->
            deleteHistoryLogsWithPage(page.id)
            deleteHistoryPage(page)
        }
    }

    /**
     * ログを削除する
     */
    @Delete
    suspend fun deleteHistoryLog(item: HistoryLog)

    /**
     * 該当のページIDをもつ全てのログを削除する
     */
    @Query("""
        delete from browser_history_items
        where pageId = :pageId
    """)
    suspend fun deleteHistoryLogsWithPage(pageId: Long)

    /**
     * 指定した期間内の全てのログを削除する
     */
    @Query("""
        delete from browser_history_items
        where visitedAt>=:start and visitedAt<:end
    """)
    suspend fun deleteHistory(start: LocalDateTime, end: LocalDateTime)

    // ------ //
    // CLEAR

    /**
     * 全てのログを削除する
     */
    @Query("delete from browser_history_items")
    suspend fun clearHistoryLogs()

    /**
     * 全てのページ情報を削除する
     *
     * これを使用する際にはログ情報も併せて全削除する必要があるので、外部から直接使用してはいけない
     */
    @Query("delete from browser_history_pages")
    suspend fun __clearHistoryPages()

    /**
     * 全ての閲覧履歴を削除する
     */
    @Transaction
    suspend fun clearHistory() {
        clearHistoryLogs()
        __clearHistoryPages()
    }

    // --------------- //

    @Query("""
        DELETE FROM browser_history_items 
        WHERE NOT EXISTS (SELECT * FROM browser_history_pages p WHERE p.id = browser_history_items.pageId)
    """)
    suspend fun restoreHistoryTable_v192()
}
