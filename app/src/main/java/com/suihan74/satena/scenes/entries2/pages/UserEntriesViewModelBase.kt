package com.suihan74.satena.scenes.entries2.pages

import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository

abstract class UserEntriesViewModelBase : EntriesFragmentViewModel() {
    /** ユーザーのタグ一覧 */
    abstract val tags : EntriesRepository.TagsLiveData
}
