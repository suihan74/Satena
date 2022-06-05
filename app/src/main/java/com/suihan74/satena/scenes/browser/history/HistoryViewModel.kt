package com.suihan74.satena.scenes.browser.history

import android.content.Intent
import androidx.annotation.MainThread
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.getEntryRootUrl
import com.suihan74.satena.models.browser.History
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.toSystemZonedDateTime
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HistoryViewModel(
    val repository: HistoryRepository
) : ViewModel() {

    /** 閲覧履歴 */
    private val histories : LiveData<List<History>> = repository.histories

    /** 閲覧履歴表示用データ */
    val historyRecyclerItems : LiveData<List<RecyclerState<History>>> by lazy {
        _historyRecyclerItems
    }
    private val _historyRecyclerItems = MutableLiveData<List<RecyclerState<History>>>()

    /** 検索キーワード */
    val keyword = repository.keyword

    /** キーワード入力ボックスの表示状態 */
    val keywordEditTextVisible = MutableLiveData<Boolean>()

    /** 日付表示のフォーマット */
    val dateFormatter : DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("uuuu年MM月dd日")
    }

    // ------ //

    @MainThread
    fun onCreateView(owner: LifecycleOwner) {
        histories.observe(owner, Observer {
            viewModelScope.launch {
                createDisplayItems(it)
            }
        })
    }

    // ------ //

    /** ページを遷移する */
    fun goAddress(url: String, browserActivity: BrowserActivity) {
        val activityViewModel = browserActivity.viewModel
        activityViewModel.goAddress(url)
        browserActivity.closeDrawer()
    }

    /** 履歴をロードし直す */
    fun loadHistories() = viewModelScope.launch {
        repository.loadHistories()
    }

    /** 履歴の続きを取得する */
    suspend fun loadAdditional() {
        repository.loadAdditional()
    }

    // ------ //

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun createDisplayItems(histories: List<History>) = withContext(Dispatchers.Default) {
        // 日付ごとに区切りを表示する
        val states = buildList {
            var currentDate: LocalDate? = null
            histories.sortedByDescending { it.log.visitedAt }.forEach { item ->
                val itemDate = item.log.visitedAt.toSystemZonedDateTime("UTC").toLocalDate()
                if (currentDate?.equals(itemDate) != true) {
                    currentDate = itemDate
                    add(RecyclerState(
                        type = RecyclerType.SECTION,
                        extra = itemDate
                    ))
                }

                add(RecyclerState(
                    type = RecyclerType.BODY,
                    body = item
                ))
            }
            add(RecyclerState(RecyclerType.FOOTER))
            Unit
        }
        _historyRecyclerItems.postValue(states)
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
        val dialog = HistoryMenuDialog.createInstance(targetSite)
        dialog.setOnOpenListener { history ->
            activity.viewModel.goAddress(history.page.page.url)
        }

        dialog.setOnOpenBookmarksListener { history ->
            val intent = Intent(activity, BookmarksActivity::class.java).apply {
                putExtra(BookmarksActivity.EXTRA_ENTRY_URL, history.page.page.url)
            }
            activity.startActivity(intent)
        }

        dialog.setOnOpenEntriesListener { history ->
            viewModelScope.launch {
                val rootUrl = getEntryRootUrl(history.page.page.url)
                val intent = Intent(activity, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_SITE_URL, rootUrl)
                }
                activity.startActivity(intent)
            }
        }

        dialog.setOnDeleteListener { site ->
            viewModelScope.launch(Dispatchers.Main) {
                repository.deleteHistory(site)
                activity.showToast(R.string.msg_browser_removed_history)
            }
        }

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_MENU)
    }

    /** 日付を指定して履歴を削除する(かを確認してから行う) */
    fun openClearByDateDialog(date: LocalDate, fragmentManager: FragmentManager) {
        val context = SatenaApplication.instance
        val dialog = AlertDialogFragment.Builder()
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
