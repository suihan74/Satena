package com.suihan74.satena.scenes.preferences.ignored

import android.util.Log
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class IgnoredEntryRepository(
    private val dao: IgnoredEntryDao
) {
    companion object {
        private var mIgnoredEntries: ArrayList<IgnoredEntry>? = null
    }

    val ignoredEntries : List<IgnoredEntry>
        get() = mIgnoredEntries ?: emptyList()

    suspend fun load(forceUpdate: Boolean = false) = withContext(Dispatchers.IO) {
        return@withContext if (forceUpdate || mIgnoredEntries == null) {
            mIgnoredEntries = ArrayList(dao.getAllEntries())
            ignoredEntries
        }
        else mIgnoredEntries
    }

    suspend fun add(entry: IgnoredEntry) : IgnoredEntry? = withContext(Dispatchers.IO) {
        return@withContext try {
            dao.insert(entry)
            dao.find(entry.type, entry.query)?.also {
                mIgnoredEntries?.add(it)
            }
        }
        catch (e: Throwable) {
            Log.e("IgnoredEntryVM", "the entry is duplicated")
            null
        }
    }

    suspend fun delete(entry: IgnoredEntry) = withContext(Dispatchers.IO) {
        try {
            dao.delete(entry)
            mIgnoredEntries?.remove(entry)

            return@withContext
        }
        catch (e: Throwable) {
            Log.e("IgnoredEntryVM", "the entry does not exist")
        }
    }

    suspend fun update(modified: IgnoredEntry) = withContext(Dispatchers.IO) {
        try {
            dao.update(modified)
            val idx = mIgnoredEntries?.indexOfFirst { it.id == modified.id } ?: -1
            if (idx >= 0) {
                mIgnoredEntries?.removeAt(idx)
                mIgnoredEntries?.add(idx, modified)
            }
            return@withContext
        }
        catch (e: Throwable) {
            Log.e("IgnoredEntryVM", "failed to update the entry")
        }
    }
}
