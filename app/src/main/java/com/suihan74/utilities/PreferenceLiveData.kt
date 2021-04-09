package com.suihan74.utilities

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

/**
 * SafeSharedPreferenceに紐づいたLiveData
 */
class PreferenceLiveData<PrefT, KeyT, ValueT> : SingleUpdateMutableLiveData<ValueT>
    where PrefT: SafeSharedPreferences<KeyT>,
          KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT>
{
    constructor(
        prefs: PrefT,
        key: KeyT,
        initializer: ((p: PrefT, key: KeyT)->ValueT)
    ) : super(initializer.invoke(prefs, key)) {
        this.prefs = prefs
        this.key = key
    }

    constructor(prefs: PrefT, key: KeyT) : super() {
        this.prefs = prefs
        this.key = key
    }

    // ------ //

    private val prefs : PrefT

    private val key : KeyT

    // ------ //

    @MainThread
    override fun setValue(value: ValueT?) {
        super.setValue(value)
        prefs.edit {
            put(key, value)
        }
    }

    @WorkerThread
    override fun postValue(value: ValueT?) {
        super.postValue(value)
        prefs.edit {
            put(key, value)
        }
    }

    // ----- //s

    @MainThread
    fun <DataT> setValue(value: ValueT, converter: (ValueT)->DataT) {
        super.setValue(value)
        prefs.edit {
            put(key, converter(value))
        }
    }

    @WorkerThread
    fun <DataT> postValue(value: ValueT, converter: (ValueT)->DataT) {
        super.postValue(value)
        prefs.edit {
            put(key, converter(value))
        }
    }
}
