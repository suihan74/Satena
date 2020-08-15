package com.suihan74.satena.scenes.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ViewModel(
    private val repository: Repository
) : ViewModel() {
    /** バージョン名 */
    val appVersion = repository.appVersion

    fun start(onError: OnError? = null) = viewModelScope.launch(Dispatchers.Default) {
        repository.start(onError)
    }

    class Factory(
        private val repository: Repository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) : T =
            ViewModel(repository) as T
    }
}
