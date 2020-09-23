package com.suihan74.utilities

/** SafeSharedPreferenceに紐づいたLiveData */
class PreferenceLiveData<PrefT, KeyT, ValueT>(
    prefs: PrefT,
    key: KeyT,
    initializer: ((p: PrefT, key: KeyT)->ValueT)? = null
) : SingleUpdateMutableLiveData<ValueT>(initializer?.invoke(prefs, key))
        where PrefT: SafeSharedPreferences<KeyT>,
              KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT>
{
    init {
        observeForever {
            prefs.edit {
                put(key, it)
            }
        }
    }
}
