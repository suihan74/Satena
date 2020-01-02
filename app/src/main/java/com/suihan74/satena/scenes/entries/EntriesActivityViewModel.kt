package com.suihan74.satena.scenes.entries

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryRepository
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntriesActivityViewModel(
    private val ignoredEntryRepository: IgnoredEntryRepository
) : ViewModel() {

    val ignoredEntries: MutableLiveData<List<IgnoredEntry>> by lazy {
        MutableLiveData<List<IgnoredEntry>>()
    }

    fun load() = viewModelScope.launch {
        ignoredEntries.postValue(ignoredEntryRepository.load())
    }

    fun addIgnoredEntry(
        entry: IgnoredEntry,
        onSuccess: ((IgnoredEntry)->Unit)? = null,
        onError: CompletionHandler? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        try {
            ignoredEntryRepository.add(entry)
            ignoredEntries.value = ignoredEntryRepository.ignoredEntries
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }

        onSuccess?.invoke(entry)
    }

    class Factory(private val ignoredEntryRepository: IgnoredEntryRepository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            EntriesActivityViewModel(ignoredEntryRepository) as T
    }
}
