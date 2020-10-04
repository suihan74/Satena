package com.suihan74.satena.models.browser

import androidx.room.*
import org.threeten.bp.LocalDateTime

@Dao
interface BrowserDao {

    // --- history --- //

    @Query("select * from history order by lastVisited asc")
    suspend fun getAllHistory(): List<History>

    @Query("""
        select * from history 
        order by lastVisited desc
        limit :offset, :limit
        """)
    suspend fun getRecentHistories(offset: Int = 0, limit: Int = 10) : List<History>

    @Query("""
        select * from history 
        where url=:url
        limit 1
        """)
    suspend fun findHistory(url: String): History?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: History)

    @Delete
    suspend fun deleteHistory(history: History)

    @Query("""
        delete from history
        where lastVisited>=:start and lastVisited<:end
    """)
    suspend fun deleteHistory(start: LocalDateTime, end: LocalDateTime)

    @Query("delete from history")
    suspend fun clearHistory()

    // --------------- //
}
