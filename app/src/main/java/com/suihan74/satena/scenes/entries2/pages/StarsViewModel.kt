package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabType

class StarsViewModel(
    private val repository : EntriesRepository
) : EntriesFragmentViewModel() {
    /** 内包するタブ */
    private val tabs = EntriesTabType.getTabs(Category.Stars)

    override val tabCount: Int = tabs.size
    override fun getTabTitle(context: Context, position: Int) : String =
        context.getString(tabs[position].textId)
}
