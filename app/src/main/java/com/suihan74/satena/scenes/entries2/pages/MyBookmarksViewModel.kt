package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.TagsLiveDataContainer

class MyBookmarksViewModel(
    private val repository : EntriesRepository
) : EntriesFragmentViewModel(), TagsLiveDataContainer {
    private val tabTitles = arrayOf(
        R.string.entries_tab_mybookmarks,
        R.string.entries_tab_read_later
    )

    /** ユーザーのタグ一覧 */
    override val tags by lazy {
        repository.TagsLiveData()
    }

    override val tabCount: Int = 2
    override fun getTabTitle(context: Context, position: Int) : String =
        if (position == 0 && tag.value != null) tag.value?.text ?: ""
        else context.getString(tabTitles[position])

    class Factory(
        private val repository : EntriesRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            MyBookmarksViewModel(repository) as T
    }
}
