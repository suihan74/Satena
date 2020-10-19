package com.suihan74.satena.scenes.browser.favorites

import android.app.Activity
import android.net.Uri
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesFragment
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesViewModelInterface
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.faviconUrl

class FavoriteSitesActionsImplForBrowser : FavoriteSitesViewModelInterface {
    override fun onClickItem(
        site: FavoriteSite,
        activity: Activity,
        fragment: FavoriteSitesFragment
    ) {
        activity.alsoAs<BrowserActivity> { browserActivity ->
            browserActivity.viewModel.goAddress(site.url)
            browserActivity.closeDrawer()
        }
    }

    override fun onLongClickItem(
        site: FavoriteSite,
        activity: Activity,
        fragment: FavoriteSitesFragment
    ) {
        activity.alsoAs<BrowserActivity> { browserActivity ->
            fragment.viewModel.openMenuDialog(
                browserActivity,
                site,
                fragment.childFragmentManager
            )
        }
    }

    override fun onClickAddButton(
        activity: Activity,
        fragment: FavoriteSitesFragment
    ) {
        activity.alsoAs<BrowserActivity> { browserActivity ->
            val vm = browserActivity.viewModel
            val url = vm.url.value ?: ""
            val site = FavoriteSite(
                url = url,
                title = vm.title.value ?: url,
                faviconUrl = Uri.parse(url).faviconUrl,
                isEnabled = false
            )
            fragment.viewModel.openItemRegistrationDialog(
                site,
                fragment.childFragmentManager
            )
        }
    }

    override fun openInBrowser(site: FavoriteSite, activity: Activity) {
        activity.alsoAs<BrowserActivity> { browserActivity ->
            browserActivity.openUrl(site.url)
        }
    }
}
