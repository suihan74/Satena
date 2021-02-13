package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import androidx.fragment.app.Fragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.utilities.SafeSharedPreferences

abstract class ListPreferencesViewModel(
    context: Context
) : PreferencesViewModel<PreferenceKey>(SafeSharedPreferences.create(context)) {
    abstract fun createList(
        activity: PreferencesActivity,
        fragment: Fragment
    ) : List<PreferencesAdapter.Item>
}
