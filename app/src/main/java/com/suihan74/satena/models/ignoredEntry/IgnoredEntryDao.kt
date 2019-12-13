package com.suihan74.satena.models.ignoredEntry

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update

@Dao
interface IgnoredEntryDao {
    @Insert
    fun insert(entry: IgnoredEntry)

    @Update
    fun update(entry: IgnoredEntry)

    @Delete
    fun delete(entry: IgnoredEntry)
}
