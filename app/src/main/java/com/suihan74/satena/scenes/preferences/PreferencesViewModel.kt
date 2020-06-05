package com.suihan74.satena.scenes.preferences

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

abstract class PreferencesViewModel(
    val prefs: SafeSharedPreferences<PreferenceKey>
) : ViewModel() {
    /** SafeSharedPreferencesと紐づいたLiveDataを作成する */
    protected inline fun <reified T> createLiveData(key: PreferenceKey) =
        MutableLiveData<T>(prefs.get<T>(key)).apply {
            observeForever {
                prefs.edit {
                    put(key, it)
                }
            }
        }
}
