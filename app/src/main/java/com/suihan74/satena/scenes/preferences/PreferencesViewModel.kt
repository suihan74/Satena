package com.suihan74.satena.scenes.preferences

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.getEnumConstants

abstract class PreferencesViewModel<KeyT> (
    val prefs: SafeSharedPreferences<KeyT>
) : ViewModel() where KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT> {

    /** SafeSharedPreferencesと紐づいたLiveDataを作成する */
    protected inline fun <KeyT, reified ValueT> createLiveData(
        prefs: SafeSharedPreferences<KeyT>,
        key: KeyT
    ) where KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT> =
        MutableLiveData(prefs.get<ValueT>(key)).apply {
            observeForever {
                prefs.edit {
                    put(key, it)
                }
            }
        }

    /** (enum用)SafeSharedPreferencesと紐づいたLiveDataを作成する */
    protected inline fun <KeyT, reified ValueT : Enum<ValueT>> createLiveDataEnum(
        prefs: SafeSharedPreferences<KeyT>,
        key: KeyT,
        noinline toIntConverter: ((ValueT) -> Int)? = null,
        noinline fromIntConverter: ((Int) -> ValueT)? = null
    ) : MutableLiveData<ValueT> where KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT> {
        val initialInt = prefs.getInt(key)
        val initialValue = fromIntConverter?.invoke(initialInt)
            ?: ValueT::class.getEnumConstants().firstOrNull { it.ordinal == initialInt }

        return MutableLiveData<ValueT>(initialValue).apply {
            observeForever {
                prefs.edit {
                    put(key, toIntConverter?.invoke(it) ?: it.ordinal)
                }
            }
        }
    }

    protected inline fun <reified ValueT> createLiveData(key: KeyT) =
        createLiveData<KeyT, ValueT>(prefs, key)

    protected inline fun <reified ValueT : Enum<ValueT>> createLiveDataEnum(key: KeyT) =
        createLiveDataEnum<KeyT, ValueT>(prefs, key)


    protected inline fun <reified ValueT : Enum<ValueT>> createLiveDataEnum(
        key: KeyT,
        noinline toIntConverter: ((ValueT) -> Int)? = null,
        noinline fromIntConverter: ((Int) -> ValueT)? = null
    ) = createLiveDataEnum(prefs, key, toIntConverter, fromIntConverter)
}
