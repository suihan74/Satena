package com.suihan74.satena.scenes.browser.favorites

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.models.browser.BrowserDao
import com.suihan74.satena.models.favoriteSite.FavoriteSite
import com.suihan74.satena.models.favoriteSite.FavoriteSiteAndFavicon
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesViewModelInterface
import com.suihan74.satena.scenes.preferences.pages.FavoriteSitesFragment
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.faviconUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoriteSitesActionsImplForBrowser(
    private val browserDao: BrowserDao
) : FavoriteSitesViewModelInterface {
    override fun onClickItem(
        site: FavoriteSiteAndFavicon,
        activity: Activity,
        fragment: FavoriteSitesFragment
    ) {
        activity.alsoAs<BrowserActivity> { browserActivity ->
            browserActivity.viewModel.goAddress(site.site.url)
            browserActivity.closeDrawer()
        }
    }

    override fun onLongClickItem(
        site: FavoriteSiteAndFavicon,
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

    override fun onClickAddButton(activity: Activity, fragment: FavoriteSitesFragment) {
        activity.alsoAs<BrowserActivity> { browserActivity ->
            val vm = browserActivity.viewModel
            val url = vm.url.value ?: ""
            fragment.lifecycleScope.launch(Dispatchers.Main) {
                val faviconUrl =
                    browserDao.findFaviconInfo(Uri.parse(url).host!!)?.filename?.let {
                        "${activity.filesDir.absolutePath}/favicon_cache/$it"
                    } ?: Uri.parse(url).faviconUrl

                val site = FavoriteSite(
                    url,
                    vm.title.value ?: url,
                    false,
                    0L,
                    faviconUrl
                )
                fragment.viewModel.openItemRegistrationDialog(site, fragment.childFragmentManager)
            }
        }
    }

    override fun openInBrowser(site: FavoriteSiteAndFavicon, activity: Activity) {
        activity.alsoAs<BrowserActivity> { browserActivity ->
            browserActivity.openUrl(site.site.url)
        }
    }
}
