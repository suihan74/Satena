package com.suihan74.satena.scenes.preferences.ignored

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import kotlinx.coroutines.launch
import javax.inject.Inject


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
