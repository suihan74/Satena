package com.suihan74.satena.scenes.preferences.ignored

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import kotlinx.coroutines.launch


class IgnoredEntryViewModel(
    private val repository: IgnoredEntryRepository
) : ViewModel() {

    val entries by lazy {
        MutableLiveData<List<IgnoredEntry>>()
    }

    fun init() = viewModelScope.launch {
        entries.postValue(repository.load())
    }

    fun add(entry: IgnoredEntry) = viewModelScope.launch {
        val result = repository.add(entry)
        if (result != null) {
            entries.postValue(repository.ignoredEntries)
        }
    }

    fun delete(entry: IgnoredEntry) = viewModelScope.launch {
        repository.delete(entry)
        entries.postValue(repository.ignoredEntries)
    }

    fun update(entry: IgnoredEntry) = viewModelScope.launch {
        repository.update(entry)
        entries.postValue(repository.ignoredEntries)
    }

    class Factory(private val repository: IgnoredEntryRepository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            IgnoredEntryViewModel(repository) as T
    }
}
