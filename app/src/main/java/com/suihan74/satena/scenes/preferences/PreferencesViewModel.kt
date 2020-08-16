package com.suihan74.satena.scenes.preferences

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.getEnumConstants

abstract class PreferencesViewModel(
    val prefs: SafeSharedPreferences<PreferenceKey>
) : ViewModel() {
    /** SafeSharedPreferencesと紐づいたLiveDataを作成する */
    protected inline fun <reified KeyT, reified T> createLiveData(
        prefs: SafeSharedPreferences<KeyT>,
        key: KeyT
    ) where KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT> =
        MutableLiveData<T>(prefs.get<T>(key)).apply {
            observeForever {
                prefs.edit {
                    put(key, it)
                }
            }
        }

    /** (enum用)SafeSharedPreferencesと紐づいたLiveDataを作成する */
    protected inline fun <reified KeyT, reified T : Enum<T>> createLiveDataEnum(
        prefs: SafeSharedPreferences<KeyT>,
        key: KeyT,
        noinline toIntConverter: ((T) -> Int)? = null,
        noinline fromIntConverter: ((Int) -> T)? = null
    ) : MutableLiveData<T> where KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT> {
        val initialInt = prefs.getInt(key)
        val initialValue = fromIntConverter?.invoke(initialInt)
            ?: T::class.getEnumConstants().firstOrNull { it.ordinal == initialInt }

        return MutableLiveData<T>(initialValue).apply {
            observeForever {
                prefs.edit {
                    put(key, toIntConverter?.invoke(it) ?: it.ordinal)
                }
            }
        }
    }

    protected inline fun <reified T> createLiveData(key: PreferenceKey) =
        createLiveData<PreferenceKey, T>(prefs, key)

    protected inline fun <reified T : Enum<T>> createLiveDataEnum(key: PreferenceKey) =
        createLiveDataEnum<PreferenceKey, T>(prefs, key)


    protected inline fun <reified T : Enum<T>> createLiveDataEnum(
        key: PreferenceKey,
        noinline toIntConverter: ((T) -> Int)? = null,
        noinline fromIntConverter: ((Int) -> T)? = null
    ) = createLiveDataEnum(prefs, key, toIntConverter, fromIntConverter)
}
