package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import com.suihan74.satena.scenes.preferences.PreferencesAdapter

class AccountViewModel(context: Context) : ListPreferencesViewModel(context) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun createList(fragment: ListPreferencesFragment): List<PreferencesAdapter.Item> = buildList {

    }
}
