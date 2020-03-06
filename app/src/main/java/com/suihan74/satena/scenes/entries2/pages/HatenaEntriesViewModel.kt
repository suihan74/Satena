package com.suihan74.satena.scenes.entries2.pages

import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel

class HatenaEntriesViewModel : EntriesFragmentViewModel() {
    override val tabTitles = arrayOf(
        R.string.entries_tab_hot,
        R.string.entries_tab_recent
    )
}
