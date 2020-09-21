package com.suihan74.satena.models.browser

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BrowserDao {

    // --- history --- //

    @Query("select * from history order by lastVisited asc")
    suspend fun getAllHistory(): List<History>

    @Query("""
        select * from history 
        where url=:url
        limit 1
    """)
    suspend fun findHistory(url: String): History?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: History)

    @Query("delete from history")
    suspend fun clearHistory()

    // --------------- //
}
