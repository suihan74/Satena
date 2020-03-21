package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.TagsLiveDataContainer

class UserEntriesViewModel(
    private val repository : EntriesRepository
) : EntriesFragmentViewModel(),
    TagsLiveDataContainer {
    /** ユーザーのタグ一覧 */
    override val tags by lazy {
        repository.TagsLiveData().also { t ->
            user.observeForever { u ->
                t.setUser(u)
            }
        }
    }

    override val tabCount: Int = 1
    override fun getTabTitle(context: Context, position: Int) : String = ""

    class Factory(
        private val repository : EntriesRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            UserEntriesViewModel(repository) as T
    }
}
