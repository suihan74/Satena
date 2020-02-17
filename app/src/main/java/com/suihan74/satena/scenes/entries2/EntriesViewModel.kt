package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.models.Category

class EntriesViewModel(
    private val repository : EntriesRepository
) : ViewModel() {

    /** カテゴリリスト */
    val categories by lazy {
        MutableLiveData<List<Category>>()
    }

    /** 現在表示中のカテゴリ */
    val currentCategory by lazy {
        MutableLiveData<Category>()
    }

    init {
        categories.value = Category.valuesWithoutSignedIn().toList()
    }

    class Factory(private val repository: EntriesRepository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) : T =
            EntriesViewModel(repository) as T
    }
}
