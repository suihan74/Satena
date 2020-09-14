package com.suihan74.satena.scenes.preferences.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.models.FavoriteSitesKey
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.utilities.SafeSharedPreferences

class PreferencesFavoriteSitesViewModel(
    prefs: SafeSharedPreferences<FavoriteSitesKey>
) : PreferencesViewModel<FavoriteSitesKey>(prefs) {

    /** アップデート後初回起動時にリリースノートを表示する */
    val sites = createLiveData<List<FavoriteSite>>(
        FavoriteSitesKey.SITES
    )

    class Factory(
        private val prefs: SafeSharedPreferences<FavoriteSitesKey>
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            PreferencesFavoriteSitesViewModel(prefs) as T
    }
}
