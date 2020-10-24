package com.suihan74.satena.scenes.preferences.favoriteSites

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.getEntryRootUrl
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.launch

class FavoriteSitesViewModel(
    private val favoriteSitesRepo : FavoriteSitesRepository,
    actionsImpl : FavoriteSitesViewModelInterface
) :
    ViewModel(),
    FavoriteSitesViewModelInterface by actionsImpl
{

    private val DIALOG_MENU by lazy { "DIALOG_MENU" }
    private val DIALOG_ITEM_REGISTRATION by lazy { "DIALOG_ITEM_REGISTRATION" }
    private val DIALOG_ITEM_MODIFICATION by lazy { "DIALOG_ITEM_MODIFICATION" }

    /** 登録済みのお気に入りサイト */
    val sites by lazy {
        favoriteSitesRepo.favoriteSites
    }

    // ------ //

    /** メニューダイアログを開く */
    fun openMenuDialog(
        activity: Activity,
        targetSite: FavoriteSite,
        fragmentManager: FragmentManager
    ) {
        val dialog = FavoriteSiteMenuDialog.createInstance(targetSite)

        dialog.setOnOpenListener { site ->
            openInBrowser(site, activity)
        }

        dialog.setOnModifyListener { site ->
            openItemModificationDialog(site, fragmentManager)
        }

        dialog.setOnOpenEntriesListener { site ->
            viewModelScope.launch {
                val rootUrl = getEntryRootUrl(site.url)
                val intent = Intent(activity, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_SITE_URL, rootUrl)
                }
                activity.startActivity(intent)
            }
        }

        dialog.setOnDeleteListener { site ->
            favoriteSitesRepo.unfavoriteSite(site)
            activity.showToast(R.string.entry_action_unfavorite)
        }

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_MENU)
    }

    // ------ //

    /** 項目を追加する */
    fun openItemRegistrationDialog(
        targetSite: FavoriteSite?,
        fragmentManager: FragmentManager
    ) {
        val dialog = FavoriteSiteRegistrationDialog.createRegistrationInstance(targetSite)

        dialog.setOnRegisterListener { site ->
            val prevList = sites.value ?: emptyList()
            sites.value = prevList.plus(site)
        }

        dialog.setDuplicationChecker { site ->
            favoriteSitesRepo.contains(site.url)
        }

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_ITEM_REGISTRATION)
    }

    /** 項目を編集する */
    fun openItemModificationDialog(
        targetSite: FavoriteSite,
        fragmentManager: FragmentManager
    ) {
        val dialog = FavoriteSiteRegistrationDialog.createModificationInstance(targetSite)

        dialog.setOnModifyListener { site ->
            val prevList = sites.value ?: emptyList()
            sites.value = prevList
                .map {
                    if (it.url == targetSite.url) site
                    else it
                }
        }

        dialog.setDuplicationChecker { site ->
            targetSite.url != site.url && favoriteSitesRepo.contains(site.url)
        }

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_ITEM_MODIFICATION)
    }
}
