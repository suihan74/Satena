package com.suihan74.satena.scenes.browser.favorites

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.models.FavoriteSitesKey
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.PreferenceLiveData
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSiteMenuDialog
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.alsoAs
import com.suihan74.utilities.showAllowingStateLoss
import com.suihan74.utilities.showToast

class FavoriteSitesViewModel(
    private val prefs: SafeSharedPreferences<FavoriteSitesKey>
) : ViewModel() {

    private val DIALOG_MENU by lazy { "DIALOG_MENU" }

    /** 登録済みのお気に入りサイト */
    val sites =
        PreferenceLiveData(
            prefs,
            FavoriteSitesKey.SITES,
            { prefs.get<List<FavoriteSite>>(FavoriteSitesKey.SITES) }
        )

    // ------ //

    /** メニューダイアログを開く */
    fun openMenuDialog(
        targetSite: FavoriteSite,
        fragmentManager: FragmentManager
    ) {
        FavoriteSiteMenuDialog.createInstance(targetSite).run {
            setOnOpenListener { site ->
                activity.alsoAs<BrowserActivity> { activity ->
                    activity.openUrl(site.url)
                }
            }

            setOnDeleteListener { site ->
                val oldSites = sites.value ?: emptyList()
                sites.value = oldSites.filterNot { it.url == site.url }
                SatenaApplication.instance.showToast(R.string.entry_action_unfavorite)
            }

            showAllowingStateLoss(fragmentManager, DIALOG_MENU)
        }
    }
}
