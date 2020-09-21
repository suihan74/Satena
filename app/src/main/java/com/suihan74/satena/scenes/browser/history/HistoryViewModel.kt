package com.suihan74.satena.scenes.browser.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.suihan74.satena.models.browser.History
import com.suihan74.satena.scenes.browser.BrowserRepository

class HistoryViewModel(
    val repository: BrowserRepository
) : ViewModel() {

    /** 閲覧履歴 */
    val histories : LiveData<List<History>> by lazy {
        repository.histories
    }

}
