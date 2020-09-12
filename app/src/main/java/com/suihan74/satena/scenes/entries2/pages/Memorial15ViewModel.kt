package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.lifecycle.*
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesAdapter
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentViewModel
import com.suihan74.utilities.OnError
import org.threeten.bp.LocalDate

class Memorial15ViewModel : EntriesFragmentViewModel() {

    /**
     * 表示する年数(2005年開始～)
     * TODO: サーバー側が2021年以降も対応するのか不明
     */
    private val years = (2005 .. LocalDate.now().year).toList()

    override val tabCount: Int = years.size
    override fun getTabTitle(context: Context, position: Int) : String =
        context.getString(R.string.entries_tab_15th, years[position])

    /** ユーザーのブクマを表示するモードか */
    val isUserMode by lazy {
        MutableLiveData<Boolean>(false)
    }

    /** タブ用ViewModelへの値変更の伝播 */
    override fun connectToTab(
        lifecycleOwner: LifecycleOwner,
        entriesAdapter: EntriesAdapter,
        viewModel: EntriesTabFragmentViewModel,
        onError: OnError?
    ) {
        super.connectToTab(lifecycleOwner, entriesAdapter, viewModel, onError)

        // "タブの"初期ロード時に設定を反映させるために現在値を代入している
        // タブ数が3以上になると、TabViewModelの初期化時には既にFragmentViewModelの中身が更新されている可能性がある
        viewModel.isUserMemorial = isUserMode.value ?: false
        isUserMode.observe(lifecycleOwner) {
            if (category.value != Category.Memorial15th || it == null || it == viewModel.isUserMemorial) return@observe
            viewModel.isUserMemorial = it
            entriesAdapter.clearEntries {
                viewModel.reloadLists(onError = onError)
            }
        }
    }

    class Factory : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            Memorial15ViewModel() as T
    }
}
