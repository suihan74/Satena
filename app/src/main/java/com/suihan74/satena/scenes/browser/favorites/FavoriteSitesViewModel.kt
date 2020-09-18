package com.suihan74.satena.scenes.browser.favorites

import androidx.lifecycle.ViewModel
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.models.FavoriteSitesKey
import com.suihan74.satena.scenes.browser.PreferenceLiveData
import com.suihan74.utilities.SafeSharedPreferences

class FavoriteSitesViewModel(
    private val prefs: SafeSharedPreferences<FavoriteSitesKey>
) : ViewModel() {

    /** 登録済みのお気に入りサイト */
    val sites =
        PreferenceLiveData(
            prefs,
            FavoriteSitesKey.SITES,
            { prefs.get<List<FavoriteSite>>(FavoriteSitesKey.SITES) }
        )

}
