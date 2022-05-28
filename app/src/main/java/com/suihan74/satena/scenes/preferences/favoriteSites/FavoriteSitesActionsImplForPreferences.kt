package com.suihan74.satena.scenes.preferences.favoriteSites

import android.app.Activity
import android.content.Intent
import com.suihan74.satena.models.favoriteSite.FavoriteSiteAndFavicon
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.preferences.pages.FavoriteSitesFragment

class FavoriteSitesActionsImplForPreferences : FavoriteSitesViewModelInterface {
    private fun openMenuDialog(
        site: FavoriteSiteAndFavicon,
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
        site: FavoriteSiteAndFavicon,
        activity: Activity,
        fragment: FavoriteSitesFragment
    ) {
        openMenuDialog(site, activity, fragment)
    }

    override fun onLongClickItem(
        site: FavoriteSiteAndFavicon,
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

    override fun openInBrowser(site: FavoriteSiteAndFavicon, activity: Activity) {
        val intent = Intent(activity, BrowserActivity::class.java).also {
            it.putExtra(BrowserActivity.EXTRA_URL, site.site.url)
        }
        activity.startActivity(intent)
    }
}
