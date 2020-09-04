package com.suihan74.satena.scenes.preferences.ignored

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.utilities.OnError
import com.suihan74.utilities.OnSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class IgnoredEntryViewModel(
    private val repository: IgnoredEntryRepository
) : ViewModel() {

    val entries by lazy {
        MutableLiveData<List<IgnoredEntry>>()
    }

    fun init() = viewModelScope.launch {
        entries.postValue(repository.load(forceUpdate = true))
    }

    fun add(
        entry: IgnoredEntry,
        onSuccess: OnSuccess<Unit>? = null,
        onError: OnError? = null
    ) = viewModelScope.launch {
        try {
            repository.add(entry) ?: let {
                throw IllegalAccessError("failed to add an ignored entry")
            }
            entries.postValue(repository.ignoredEntries)

            withContext(Dispatchers.Main) {
                onSuccess?.invoke(Unit)
            }
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }
    }

    fun delete(entry: IgnoredEntry) = viewModelScope.launch {
        repository.delete(entry)
        entries.postValue(repository.ignoredEntries)
    }

    fun update(
        entry: IgnoredEntry,
        onSuccess: OnSuccess<Unit>? = null,
        onError: OnError? = null
    ) = viewModelScope.launch {
        try {
            repository.update(entry)
            entries.postValue(repository.ignoredEntries)

            withContext(Dispatchers.Main) {
                onSuccess?.invoke(Unit)
            }
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }
    }

    class Factory(private val repository: IgnoredEntryRepository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            IgnoredEntryViewModel(repository) as T
    }
}
