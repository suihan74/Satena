package com.suihan74.satena.models.readEntry

import androidx.room.*
import com.suihan74.hatenaLib.Entry
import java.time.ZonedDateTime

@Dao
interface ReadEntryDao {
    // ===== find ===== //

    @Query("""
        select * from read_entry
        where eid=:eid
    """)
    suspend fun find(eid: Long) : ReadEntry

    @Query("""
        select * from read_entry
        where eid in (:eidList)
    """)
    suspend fun find(eidList: List<Long>) : List<ReadEntry>

    /**
     * 存在するレコードをeidのセットに変換して取得する
     */
    @Transaction
    suspend fun exist(eidList: List<Long>) : Set<Long> =
        HashSet<Long>().also { set ->
            find(eidList).forEach { entry -> set.add(entry.eid) }
        }

    // ===== insert ===== //

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(readEntry: ReadEntry)

    @Transaction
    suspend fun insert(eid: Long) = insert(ReadEntry(eid, ZonedDateTime.now()))

    @Transaction
    suspend fun insert(entry: Entry) = insert(ReadEntry(entry.id, ZonedDateTime.now()))

    // ===== delete ===== //

    @Delete
    suspend fun delete(readEntry: ReadEntry)

    @Transaction
    suspend fun delete(entry: Entry) = delete(entry.id)

    @Query("delete from read_entry where eid = :id")
    suspend fun delete(id: Long)

    /** 指定日より前の項目を削除する */
    @Query("delete from read_entry where timestamp<:date")
    suspend fun delete(date: ZonedDateTime)

    @Query("delete from read_entry")
    suspend fun clearAll()
}
