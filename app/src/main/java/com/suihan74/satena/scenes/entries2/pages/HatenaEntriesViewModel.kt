package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabType

class HatenaEntriesViewModel(
    private val repository: EntriesRepository
) : EntriesFragmentViewModel() {
    /** 内包するタブ */
    private val tabs = EntriesTabType.getTabs(null)

    /** 現在categoryが内包するissueのリスト */
    val issues by lazy {
        repository.IssuesLiveData(category)
    }

    override val tabCount: Int = tabs.size
    override fun getTabTitle(context: Context, position: Int) =
        context.getString(tabs[position].textId)
}
