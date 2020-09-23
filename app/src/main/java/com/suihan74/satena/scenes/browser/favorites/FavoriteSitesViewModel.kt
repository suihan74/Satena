package com.suihan74.satena.scenes.browser.favorites

import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.suihan74.satena.R
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSiteMenuDialog
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss


class FavoriteSitesViewModel(
    private val favoriteSitesRepo : FavoriteSitesRepository
) : ViewModel() {

    private val DIALOG_MENU by lazy { "DIALOG_MENU" }

    /** 登録済みのお気に入りサイト */
    val sites by lazy {
        favoriteSitesRepo.sites
    }

    // ------ //

    /** メニューダイアログを開く */
    fun openMenuDialog(
        activity: BrowserActivity,
        targetSite: FavoriteSite,
        fragmentManager: FragmentManager
    ) {
        FavoriteSiteMenuDialog.createInstance(targetSite).run {
            setOnOpenListener { site ->
                activity.alsoAs<BrowserActivity> { activity ->
                    activity.openUrl(site.url)
                }
            }

            setOnOpenEntriesListener { site ->
                val intent = Intent(activity, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_SITE_URL, site.url)
                }
                activity.startActivity(intent)
            }

            setOnDeleteListener { site ->
                favoriteSitesRepo.unfavorite(site)
                activity.showToast(R.string.entry_action_unfavorite)
            }

            showAllowingStateLoss(fragmentManager, DIALOG_MENU)
        }
    }
}
