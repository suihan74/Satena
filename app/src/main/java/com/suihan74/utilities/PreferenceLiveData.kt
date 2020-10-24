package com.suihan74.utilities

/** SafeSharedPreferenceに紐づいたLiveData */
class PreferenceLiveData<PrefT, KeyT, ValueT> : SingleUpdateMutableLiveData<ValueT>
    where PrefT: SafeSharedPreferences<KeyT>,
          KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT>
{
    constructor(
        prefs: PrefT,
        key: KeyT,
        initializer: ((p: PrefT, key: KeyT)->ValueT)
    ) : super(initializer.invoke(prefs, key)) {
        notifyPrefs(prefs, key)
    }

    constructor(prefs: PrefT, key: KeyT) : super() {
        notifyPrefs(prefs, key)
    }

    private fun notifyPrefs(prefs: PrefT, key: KeyT) {
        observeForever {
            prefs.edit {
                put(key, it)
            }
        }
    }
}
