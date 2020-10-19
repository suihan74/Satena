package com.suihan74.satena.scenes.preferences.favoriteSites

import android.app.Activity
import android.content.Intent
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.scenes.browser.BrowserActivity

class FavoriteSitesActionsImplForPreferences : FavoriteSitesViewModelInterface {
    private fun openMenuDialog(
        site: FavoriteSite,
        activity: Activity,
        fragment: FavoriteSitesFragment
    ) {
        fragment.viewModel.openMenuDialog(
            activity,
            site,
            fragment.childFragmentManager
        )
    }

    override fun onClickItem(
        site: FavoriteSite,
        activity: Activity,
        fragment: FavoriteSitesFragment
    ) {
        openMenuDialog(site, activity, fragment)
    }

    override fun onLongClickItem(
        site: FavoriteSite,
        activity: Activity,
        fragment: FavoriteSitesFragment
    ) {
        openMenuDialog(site, activity, fragment)
    }

    override fun onClickAddButton(
        activity: Activity,
        fragment: FavoriteSitesFragment
    ) {
        fragment.viewModel.openItemRegistrationDialog(
            null,
            fragment.childFragmentManager
        )
    }

    override fun openInBrowser(site: FavoriteSite, activity: Activity) {
        val intent = Intent(activity, BrowserActivity::class.java).also {
            it.putExtra(BrowserActivity.EXTRA_URL, site.url)
        }
        activity.startActivity(intent)
    }
}
