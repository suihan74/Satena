package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.*
import com.suihan74.utilities.OnError

class Memorial15ViewModel : EntriesFragmentViewModel() {

    /**
     * 表示する年数(2005年開始～)
     * サーバー側が2021年以降は対応しないっぽいので2020年までだけを取得する
     */
    private val years = (2005 .. 2020).toList()

    override val tabCount: Int = years.size
    override fun getTabTitle(context: Context, position: Int) : String =
        context.getString(R.string.entries_tab_15th, years[position])

    /** ユーザーのブクマを表示するモードか */
    val isUserMode = MutableLiveData(false)

    /** タブ用ViewModelへの値変更の伝播 */
    override fun connectToTab(
        fragment: EntriesTabFragment,
        entriesAdapter: EntriesAdapter,
        viewModel: EntriesTabFragmentViewModel,
        onError: OnError?
    ) {
        super.connectToTab(fragment, entriesAdapter, viewModel, onError)

        // "タブの"初期ロード時に設定を反映させるために現在値を代入している
        // タブ数が3以上になると、TabViewModelの初期化時には既にFragmentViewModelの中身が更新されている可能性がある
        viewModel.isUserMemorial = isUserMode.value ?: false
        isUserMode.observe(fragment.viewLifecycleOwner, Observer {
            if (category.value != Category.Memorial15th || it == null || it == viewModel.isUserMemorial) return@Observer
            viewModel.isUserMemorial = it
            entriesAdapter.clearEntries {
                fragment.lifecycleScope.launchWhenResumed {
                    runCatching { viewModel.reloadLists() }
                        .onFailure { e-> onError?.invoke(e) }
                }
            }
        })
    }
}
