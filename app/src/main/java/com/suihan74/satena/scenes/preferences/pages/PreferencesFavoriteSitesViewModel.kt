package com.suihan74.satena.scenes.preferences.pages

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.suihan74.satena.R
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.scenes.browser.favorites.FavoriteSitesRepository
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSiteMenuDialog
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss


class PreferencesFavoriteSitesViewModel(
    private val favoriteSitesRepo : FavoriteSitesRepository
) : ViewModel() {

    private val DIALOG_MENU by lazy { "DIALOG_MENU" }

    /** お気に入りサイトリスト */
    val sites by lazy {
        favoriteSitesRepo.sites
    }

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
                favoriteSitesRepo.unfavorite(site)
                activity.showToast(R.string.entry_action_unfavorite)
            }

            showAllowingStateLoss(fragmentManager, DIALOG_MENU)
        }
    }
}
