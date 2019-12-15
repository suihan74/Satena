package com.suihan74.satena.models.ignoredEntry

import androidx.room.*

@Dao
abstract class IgnoredEntryDao {
    @Query("select * from ignored_entry")
    abstract fun getAllEntries(): List<IgnoredEntry>

    @Query("""
        select * from ignored_entry 
        where type = :typeInt and `query` = :query 
        limit 1
    """)
    abstract fun find(typeInt: Int, query: String) : IgnoredEntry?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insert(entry: IgnoredEntry)

    @Update
    abstract fun update(entry: IgnoredEntry)

    @Delete
    abstract fun delete(entry: IgnoredEntry)

    // 外側から使いやすくするためのもの //

    fun find(type: IgnoredEntryType, query: String) =
        find(type.ordinal, query)

    fun clearAllEntries() {
        getAllEntries().forEach {
            delete(it)
        }
    }
}

