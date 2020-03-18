package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suihan74.hatenaLib.Issue
import com.suihan74.hatenaLib.Tag
import com.suihan74.satena.models.Category
import com.suihan74.utilities.SingleUpdateMutableLiveData

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

    /** この画面で表示しているタグ(Category.MyBookmarks) */
    val tag by lazy {
        SingleUpdateMutableLiveData<Tag?>(
            selector = { it?.text }
        )
    }

    /** エントリリストを取得するサイトURL */
    val siteUrl by lazy {
        SingleUpdateMutableLiveData<String>()
    }

    // タブ管理に関する設定

    abstract val tabTitles : Array<Int>
    fun getTabTitleId(position: Int) = tabTitles[position]
    val tabCount: Int
        get() = tabTitles.size

    // タブ設定に関する設定ここまで
}
