package com.suihan74.satena.scenes.preferences.ignored

import android.util.Log
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class IgnoredEntryRepository {
    @Inject
    lateinit var dao: IgnoredEntryDao

    suspend fun load() = withContext(Dispatchers.IO) {
        return@withContext dao.getAllEntries()
    }

    suspend fun add(entry: IgnoredEntry) : IgnoredEntry? = withContext(Dispatchers.IO) {
        return@withContext try {
            dao.insert(entry)
            dao.find(entry.type, entry.query)
        }
        catch (e: Exception) {
            Log.e("IgnoredEntryVM", "the entry is duplicated")
            null
        }
    }

    suspend fun delete(entry: IgnoredEntry) = withContext(Dispatchers.IO) {
        try {
            dao.delete(entry)
            return@withContext
        }
        catch (e: Exception) {
            Log.e("IgnoredEntryVM", "the entry does not exist")
        }
    }

    suspend fun update(modified: IgnoredEntry) = withContext(Dispatchers.IO) {
        try {
            dao.update(modified)
        }
        catch (e: Exception) {
            Log.e("IgnoredEntryVM", "failed to update the entry")
        }
    }

    init {
        SatenaApplication.instance.component.inject(this)
    }
}
