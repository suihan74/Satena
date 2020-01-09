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

    fun find(type: IgnoredEntryType, query: String) =
        find(type.ordinal, query)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insert(entry: IgnoredEntry)

    @Update
    abstract fun update(entry: IgnoredEntry)

    @Query("""delete from ignored_entry
        where type = :typeInt and `query` = :query
    """)
    abstract fun delete(typeInt: Int, query: String)

    fun delete(entry: IgnoredEntry) =
        delete(entry.type.ordinal, entry.query)

    fun clearAllEntries() {
        getAllEntries().forEach {
            delete(it)
        }
    }
}

