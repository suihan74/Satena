package com.suihan74.satena.scenes.browser.history

import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.suihan74.satena.R
import com.suihan74.satena.models.browser.History
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss

class HistoryViewModel(
    val repository: HistoryRepository
) : ViewModel() {

    private val DIALOG_MENU by lazy { "DIALOG_MENU" }

    /** 閲覧履歴 */
    val histories : LiveData<List<History>> by lazy {
        repository.histories
    }

    // ------ //

    /** ページを遷移する */
    fun goAddress(url: String, browserActivity: BrowserActivity) {
        val activityViewModel = browserActivity.viewModel
        activityViewModel.goAddress(url)
        browserActivity.closeDrawer()
    }

    /** 履歴の続きを取得する */
    suspend fun loadAdditional() {
        repository.loadAdditional()
    }

    /** 項目に対するメニューダイアログを開く */
    fun openItemMenuDialog(targetSite: History, activity: BrowserActivity, fragmentManager: FragmentManager) {
        HistoryMenuDialog.createInstance(targetSite).run {
            setOnOpenListener { site ->
                activity.viewModel.goAddress(site.url)
            }

            setOnOpenEntriesListener { site ->
                val intent = Intent(activity, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_SITE_URL, site.url)
                }
                activity.startActivity(intent)
            }

            setOnFavoriteListener { site ->
                // TODO
            }

            setOnDeleteListener { site ->
                val oldSites = histories.value ?: emptyList()
                repository.histories.value = oldSites.filterNot { it.url == site.url }
                activity.showToast(R.string.entry_action_unfavorite)
            }

            showAllowingStateLoss(fragmentManager, DIALOG_MENU)
        }
    }
}
