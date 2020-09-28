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
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSiteRegistrationDialog
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss


class PreferencesFavoriteSitesViewModel(
    private val favoriteSitesRepo : FavoriteSitesRepository
) : ViewModel() {

    private val DIALOG_MENU by lazy { "DIALOG_MENU" }
    private val DIALOG_ITEM_MODIFICATION by lazy { "DIALOG_ITEM_MODIFICATION" }

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

            setOnModifyListener { site ->
                openItemModificationDialog(site, fragmentManager)
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

    // ------ //

    /** 項目を編集する */
    fun openItemModificationDialog(
        targetSite: FavoriteSite,
        fragmentManager: FragmentManager
    ) {
        FavoriteSiteRegistrationDialog.createInstance(
            mode = FavoriteSiteRegistrationDialog.Mode.MODIFY,
            url = targetSite.url,
            title = targetSite.title,
            faviconUrl = targetSite.faviconUrl
        ).let { dialog ->
            dialog.setOnModifyListener { site ->
                val prevList = sites.value ?: emptyList()
                sites.value = prevList
                    .filterNot { it.url == targetSite.url }
                    .plus(site)
            }

            dialog.setDuplicationChecker { site ->
                targetSite.url == site.url ||
                sites.value?.none { it.url == site.url } ?: true
            }

            dialog.showAllowingStateLoss(fragmentManager, DIALOG_ITEM_MODIFICATION)
        }
    }
}
