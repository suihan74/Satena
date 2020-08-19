package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.lifecycle.*
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesAdapter
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentViewModel
import org.threeten.bp.LocalDate

class Memorial15ViewModel(
    private val repository: EntriesRepository
) : EntriesFragmentViewModel() {

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
        onError: ((Throwable) -> Unit)?
    ) {
        super.connectToTab(lifecycleOwner, entriesAdapter, viewModel, onError)

        isUserMode.observe(lifecycleOwner) {
            if (category.value != Category.Memorial15th || it == null || it == viewModel.isUserMemorial) return@observe
            viewModel.isUserMemorial = it
            entriesAdapter.clearEntries {
                viewModel.refresh(onError)
            }
        }
    }

    class Factory(
        private val repository : EntriesRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            Memorial15ViewModel(repository) as T
    }
}
