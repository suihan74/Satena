package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel

class FavoriteSitesViewModel : EntriesFragmentViewModel() {
    private val tabTitles = arrayOf(
        R.string.entries_tab_hot,
        R.string.entries_tab_recent
    )

    override val tabCount: Int = 2
    override fun getTabTitle(context: Context, position: Int): String =
        context.getString(tabTitles[position])
}
