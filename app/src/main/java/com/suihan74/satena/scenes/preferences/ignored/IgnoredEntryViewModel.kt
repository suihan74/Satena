package com.suihan74.satena.scenes.preferences.ignored

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Module
class IgnoredEntryModule(private val app: SatenaApplication) {
    @Provides
    fun provideIgnoredEntryDao() =
        app.ignoredEntryDao

    @Provides
    fun provideIgnoredEntryRepository() =
        IgnoredEntryRepository()
}

class IgnoredEntryRepository {
    @Inject lateinit var dao: IgnoredEntryDao

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

class IgnoredEntryViewModel : ViewModel() {

    @Inject lateinit var repository: IgnoredEntryRepository

    val entries: MutableLiveData<List<IgnoredEntry>> by lazy {
        MutableLiveData<List<IgnoredEntry>>()
    }

    fun init() = viewModelScope.launch {
        entries.postValue(repository.load())
    }

    fun add(entry: IgnoredEntry) = viewModelScope.launch {
        val result = repository.add(entry)
        if (result != null) {
            val items = entries.value
            entries.postValue(
                items?.plusElement(result) ?: listOf(result)
            )
        }
    }

    fun delete(entry: IgnoredEntry) = viewModelScope.launch {
        repository.delete(entry)
        entries.postValue(
            entries.value?.filterNot { it == entry }
        )
    }

    fun update(entry: IgnoredEntry) = viewModelScope.launch {
        repository.update(entry)
        entries.postValue(repository.load())
    }

    init {
        SatenaApplication.instance.component.inject(this)
    }
}
