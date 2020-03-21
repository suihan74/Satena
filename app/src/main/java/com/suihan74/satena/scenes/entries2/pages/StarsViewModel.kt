package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository

class StarsViewModel(
    private val repository : EntriesRepository
) : EntriesFragmentViewModel() {
    private val tabTitles = arrayOf(
        R.string.entries_tab_my_stars,
        R.string.entries_tab_stars_report
    )

    override val tabCount: Int = 2
    override fun getTabTitle(context: Context, position: Int) : String = context.getString(tabTitles[position])

    class Factory(
        private val repository : EntriesRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            StarsViewModel(repository) as T
    }
}
