package com.suihan74.satena.models.ignoredEntry

import androidx.room.*

@Dao
interface IgnoredEntryDao {
    @Query("select * from ignored_entry")
    fun getAllEntries(): List<IgnoredEntry>

    @Query("""
        select * from ignored_entry
        where target = :bookmarkInt or target = :allInt
    """)
    fun getEntriesForBookmarks(
        bookmarkInt: Int = IgnoreTarget.BOOKMARK.int,
        allInt: Int = IgnoreTarget.ALL.int
    ) : List<IgnoredEntry>

    @Query("""
        select * from ignored_entry 
        where type = :typeInt and `query` = :query
        limit 1
    """)
    fun find(typeInt: Int, query: String) : IgnoredEntry?

    fun find(type: IgnoredEntryType, query: String) =
        find(type.ordinal, query)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(entry: IgnoredEntry)

    @Update
    fun update(entry: IgnoredEntry)

    @Query("""delete from ignored_entry
        where type = :typeInt and `query` = :query
    """)
    fun delete(typeInt: Int, query: String)

    fun delete(entry: IgnoredEntry) =
        delete(entry.type.ordinal, entry.query)

    fun clearAllEntries() {
        getAllEntries().forEach {
            delete(it)
        }
    }
}

