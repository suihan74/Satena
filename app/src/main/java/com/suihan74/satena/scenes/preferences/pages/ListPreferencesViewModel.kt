package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.utilities.SafeSharedPreferences

abstract class ListPreferencesViewModel(
    context: Context
) : PreferencesViewModel<PreferenceKey>(SafeSharedPreferences.create(context)) {
    abstract fun createList(fragmentManager: FragmentManager) : List<PreferencesAdapter.Item>
}
