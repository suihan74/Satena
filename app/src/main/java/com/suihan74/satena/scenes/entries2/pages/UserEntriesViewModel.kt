package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
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
                if (u != null) t.setUser(u)
            }
        }
    }

    override val tabCount: Int = 1
    override fun getTabTitle(context: Context, position: Int) : String = ""
}
