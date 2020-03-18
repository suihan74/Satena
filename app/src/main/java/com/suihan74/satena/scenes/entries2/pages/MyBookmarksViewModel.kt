package com.suihan74.satena.scenes.entries2.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository

class MyBookmarksViewModel(
    private val user : String,
    private val repository : EntriesRepository
) : EntriesFragmentViewModel() {
    override val tabTitles = arrayOf(
        R.string.entries_tab_mybookmarks,
        R.string.entries_tab_read_later
    )

    /** ユーザーのタグ一覧 */
    val tags by lazy {
        repository.TagsLiveData(user)
    }

    class Factory(
        private val user : String,
        private val repository : EntriesRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            MyBookmarksViewModel(user, repository) as T
    }
}
