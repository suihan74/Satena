package com.suihan74.satena.scenes.preferences.ignored

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.utilities.OnError
import com.suihan74.utilities.OnSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IgnoredEntryViewModel(
    val repository: IgnoredEntriesRepository
) : ViewModel() {

    val entries by lazy {
        repository.ignoredEntries
    }

    // ------ //

    init {
        viewModelScope.launch {
            repository.loadAllIgnoredEntries(forceUpdate = true)
        }
    }

    // ------ //

    fun add(
        entry: IgnoredEntry,
        onSuccess: OnSuccess<Unit>? = null,
        onError: OnError? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        try {
            repository.addIgnoredEntry(entry)
            onSuccess?.invoke(Unit)
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }
    }

    fun delete(entry: IgnoredEntry) = viewModelScope.launch {
        runCatching {
            repository.deleteIgnoredEntry(entry)
        }
    }

    fun update(
        entry: IgnoredEntry,
        onSuccess: OnSuccess<Unit>? = null,
        onError: OnError? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        try {
            repository.updateIgnoredEntry(entry)
            onSuccess?.invoke(Unit)
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }
    }
}
