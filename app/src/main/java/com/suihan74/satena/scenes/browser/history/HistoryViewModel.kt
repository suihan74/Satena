package com.suihan74.satena.scenes.browser.history

import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.models.browser.History
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryViewModel(
    val repository: HistoryRepository
) : ViewModel() {

    private val DIALOG_MENU by lazy { "DIALOG_MENU" }

    /** 閲覧履歴 */
    val histories : LiveData<List<History>> by lazy {
        repository.histories
    }

    /** 検索キーワード */
    val keyword by lazy {
        repository.keyword.also {
            it.observeForever {
                viewModelScope.launch {
                    repository.updateHistoriesLiveData()
                }
            }
        }
    }

    /** キーワード入力ボックスの表示状態 */
    val keywordEditTextVisible by lazy {
        MutableLiveData<Boolean>(false)
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
                viewModelScope.launch(Dispatchers.Main) {
                    repository.deleteHistory(site)
                    activity.showToast(R.string.msg_browser_removed_history)
                }
            }

            showAllowingStateLoss(fragmentManager, DIALOG_MENU)
        }
    }
}
