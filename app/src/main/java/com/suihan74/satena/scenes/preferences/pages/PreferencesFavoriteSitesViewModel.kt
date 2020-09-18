package com.suihan74.satena.scenes.preferences.pages

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.FragmentManager
import com.suihan74.satena.R
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.models.FavoriteSitesKey
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSiteMenuDialog
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showAllowingStateLoss
import com.suihan74.utilities.showToast

class PreferencesFavoriteSitesViewModel(
    prefs: SafeSharedPreferences<FavoriteSitesKey>
) : PreferencesViewModel<FavoriteSitesKey>(prefs) {

    private val DIALOG_MENU by lazy { "DIALOG_MENU" }

    /** アップデート後初回起動時にリリースノートを表示する */
    val sites = createLiveData<List<FavoriteSite>>(
        FavoriteSitesKey.SITES
    )

    // ------ //

    /** メニューダイアログを開く */
    fun openMenuDialog(
        activity: Activity,
        targetSite: FavoriteSite,
        fragmentManager: FragmentManager
    ) {
        FavoriteSiteMenuDialog.createInstance(targetSite).run {
            setOnOpenListener { site ->
                activity.startInnerBrowser(site.url)
            }

            setOnOpenEntriesListener { site ->
                val intent = Intent(activity, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_SITE_URL, site.url)
                }
                activity.startActivity(intent)
            }

            setOnDeleteListener { site ->
                val oldSites = sites.value ?: emptyList()
                sites.value = oldSites.filterNot { it.url == site.url }
                activity.showToast(R.string.entry_action_unfavorite)
            }

            showAllowingStateLoss(fragmentManager, DIALOG_MENU)
        }
    }
}
