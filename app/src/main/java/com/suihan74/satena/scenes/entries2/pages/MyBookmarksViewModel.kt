package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.R
import com.suihan74.satena.models.EntriesTabType
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository

class MyBookmarksViewModel(
    private val user : String,
    private val repository : EntriesRepository
) : EntriesFragmentViewModel() {
    private val tabTitles = arrayOf(
        R.string.entries_tab_mybookmarks,
        R.string.entries_tab_read_later
    )

    /** ユーザーのタグ一覧 */
    val tags by lazy {
        repository.TagsLiveData(user)
    }

    override val tabCount: Int = 2
    override fun getTabTitle(context: Context, position: Int) : String =
        if (position == EntriesTabType.MYBOOKMARKS.ordinal && tag.value != null) tag.value?.text ?: ""
        else context.getString(tabTitles[position])

    class Factory(
        private val user : String,
        private val repository : EntriesRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            MyBookmarksViewModel(user, repository) as T
    }
}
