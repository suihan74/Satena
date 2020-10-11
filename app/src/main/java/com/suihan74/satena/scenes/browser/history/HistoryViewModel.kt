package com.suihan74.satena.scenes.browser.history

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment2
import com.suihan74.satena.models.browser.History
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

class HistoryViewModel(
    val repository: HistoryRepository
) : ViewModel() {

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

    /** 日付表示のフォーマット */
    val dateFormatter : DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("uuuu年MM月dd日")
    }

    // ------ //

    init {
        viewModelScope.launch {
            repository.loadHistories()
        }
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

    // ------ //

    private val DIALOG_MENU by lazy { "DIALOG_MENU" }
    private val DIALOG_CLEAR_DATE by lazy { "DIALOG_CLEAR_DATE" }

    /** 項目に対するメニューダイアログを開く */
    fun openItemMenuDialog(
        targetSite: History,
        activity: BrowserActivity,
        fragmentManager: FragmentManager
    ) {
        HistoryMenuDialog.createInstance(targetSite).run {
            setOnOpenListener { site ->
                activity.viewModel.goAddress(site.url)
            }

            setOnOpenBookmarksListener { site ->
                val intent = Intent(activity, BookmarksActivity::class.java).apply {
                    putExtra(BookmarksActivity.EXTRA_ENTRY_URL, site.url)
                }
                activity.startActivity(intent)
            }

            setOnOpenEntriesListener { site ->
                viewModelScope.launch {
                    val modifiedUrl = modifySpecificUrls(site.url) ?: site.url
                    val intent = Intent(activity, EntriesActivity::class.java).apply {
                        val uri = Uri.parse(modifiedUrl)
                        val domainUrl = runCatching {
                            uri.scheme + "://" + uri.authority + "/"
                        }.getOrDefault(modifiedUrl)
                        putExtra(EntriesActivity.EXTRA_SITE_URL, domainUrl)
                    }
                    activity.startActivity(intent)
                }
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

    /** 日付を指定して履歴を削除する(かを確認してから行う) */
    fun openClearByDateDialog(date: LocalDate, fragmentManager: FragmentManager) {
        val context = SatenaApplication.instance
        val dialog = AlertDialogFragment2.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(context.getString(R.string.msg_browser_clear_history_by_date, date.format(dateFormatter)))
            .setNegativeButton(R.string.dialog_cancel) { it.dismiss() }
            .setPositiveButton(R.string.dialog_ok) {
                viewModelScope.launch(Dispatchers.Main) {
                    val result = runCatching {
                        repository.clearHistories(date)
                    }
                    if (result.isSuccess) {
                        context.showToast(R.string.msg_browser_removed_history)
                    }
                    it.dismiss()
                }
            }
            .dismissOnClickButton(false)
            .create()
        dialog.showAllowingStateLoss(fragmentManager, DIALOG_CLEAR_DATE)
    }
}
