package com.suihan74.satena.scenes.entries2

import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.models.EntriesHistoryKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

class EntriesRepository(
    private val client: HatenaClient,
    private val prefs: SafeSharedPreferences<PreferenceKey>,
    private val historyPrefs: SafeSharedPreferences<EntriesHistoryKey>
) {

}
