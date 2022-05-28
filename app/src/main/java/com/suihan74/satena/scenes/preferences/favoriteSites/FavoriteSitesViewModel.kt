package com.suihan74.satena.scenes.preferences.favoriteSites

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.getEntryRootUrl
import com.suihan74.satena.models.favoriteSite.FavoriteSite
import com.suihan74.satena.models.favoriteSite.FavoriteSiteAndFavicon
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.showAllowingStateLoss

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
    val sites = favoriteSitesRepo.favoriteSitesFlow.asLiveData(viewModelScope.coroutineContext)

    // ------ //

    /** メニューダイアログを開く */
    fun openMenuDialog(
        activity: Activity,
        targetSite: FavoriteSiteAndFavicon,
        fragmentManager: FragmentManager
    ) {
        val dialog = FavoriteSiteMenuDialog.createInstance(targetSite)

        dialog.setOnOpenListener { site, f ->
            openInBrowser(site, f.requireActivity())
        }

        dialog.setOnModifyListener { site, f ->
            openItemModificationDialog(site.site, f.parentFragmentManager)
        }

        dialog.setOnOpenEntriesListener { site, f ->
            val rootUrl = getEntryRootUrl(site.site.url)
            val intent = Intent(activity, EntriesActivity::class.java).apply {
                putExtra(EntriesActivity.EXTRA_SITE_URL, rootUrl)
            }
            f.requireActivity().startActivity(intent)
        }

        dialog.setOnDeleteListener { site, _ ->
            val app = SatenaApplication.instance
            runCatching {
                favoriteSitesRepo.unfavoritePage(site)
            }.onSuccess {
                app.showToast(R.string.unfavorite_site_succeeded)
            }.onFailure { e ->
                app.showToast(R.string.unfavorite_site_failed)
                Log.w("unfavoriteSite", Log.getStackTraceString(e))
            }
        }

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_MENU)
    }

    // ------ //

    /** 項目を追加する */
    fun openItemRegistrationDialog(targetSite: FavoriteSite?, fragmentManager: FragmentManager) {
        val dialog = FavoriteSiteRegistrationDialog.createRegistrationInstance(targetSite)
        dialog.showAllowingStateLoss(fragmentManager, DIALOG_ITEM_REGISTRATION)
    }

    /** 項目を編集する */
    fun openItemModificationDialog(targetSite: FavoriteSite, fragmentManager: FragmentManager) {
        val dialog = FavoriteSiteRegistrationDialog.createModificationInstance(targetSite)
        dialog.showAllowingStateLoss(fragmentManager, DIALOG_ITEM_MODIFICATION)
    }
}
