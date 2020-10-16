package com.suihan74.satena.models.ignoredEntry

import androidx.room.*

@Dao
interface IgnoredEntryDao {
    @Query("select * from ignored_entry")
    suspend fun getAllEntries(): List<IgnoredEntry>

    @Query("""
        select * from ignored_entry
        where target = :bookmarkInt or target = :allInt
    """)
    suspend fun getEntriesForBookmarks(
        bookmarkInt: Int = IgnoreTarget.BOOKMARK.int,
        allInt: Int = IgnoreTarget.ALL.int
    ) : List<IgnoredEntry>

    @Query("""
        select * from ignored_entry
        where target = :entryInt or target = :allInt
    """)
    suspend fun getEntriesForEntries(
        entryInt: Int = IgnoreTarget.ENTRY.int,
        allInt: Int = IgnoreTarget.ALL.int
    ) : List<IgnoredEntry>

    @Query("""
        select * from ignored_entry 
        where type = :typeInt and `query` = :query
        limit 1
    """)
    suspend fun find(typeInt: Int, query: String) : IgnoredEntry?

    suspend fun find(type: IgnoredEntryType, query: String) =
        find(type.ordinal, query)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: IgnoredEntry)

    @Update
    suspend fun update(entry: IgnoredEntry)

    @Query("""delete from ignored_entry
        where type = :typeInt and `query` = :query
    """)
    suspend fun delete(typeInt: Int, query: String)

    suspend fun delete(entry: IgnoredEntry) =
        delete(entry.type.ordinal, entry.query)

    suspend fun clearAllEntries() {
        getAllEntries().forEach {
            delete(it)
        }
    }
}

