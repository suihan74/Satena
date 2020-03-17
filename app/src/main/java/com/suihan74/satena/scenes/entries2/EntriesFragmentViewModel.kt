package com.suihan74.satena.scenes.entries2

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Issue
import com.suihan74.satena.models.Category
import com.suihan74.utilities.SingleUpdateMutableLiveData
import kotlinx.coroutines.launch

abstract class EntriesFragmentViewModel : ViewModel() {
    /** この画面で表示しているCategory */
    val category by lazy {
        MutableLiveData<Category>()
    }

    /** この画面で表示しているIssue */
    val issue by lazy {
        SingleUpdateMutableLiveData<Issue?>(
            selector = { it?.code }
        )
    }

    /** エントリリストを取得するサイトURL */
    val siteUrl by lazy {
        SingleUpdateMutableLiveData<String>()
    }

    /** 現在categoryが内包するissueのリスト */
    val issues by lazy {
        MutableLiveData<List<Issue>?>(null).also { issuesLiveData ->
            category.observeForever { c ->
                if (!c.hasIssues) return@observeForever
                val apiCategory = c.categoryInApi ?: return@observeForever
                viewModelScope.launch {
                    try {
                        val issues = HatenaClient.getIssuesAsync(apiCategory).await()
                        issuesLiveData.postValue(issues)
                    }
                    catch (e: Throwable) {
                        Log.e("error", Log.getStackTraceString(e))
                    }
                }
            }
        }
    }

    // タブ管理に関する設定

    abstract val tabTitles : Array<Int>
    fun getTabTitleId(position: Int) = tabTitles[position]
    val tabCount: Int
        get() = tabTitles.size

    // タブ設定に関する設定ここまで
}
