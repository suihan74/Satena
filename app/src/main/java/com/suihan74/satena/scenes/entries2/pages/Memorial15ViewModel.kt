package com.suihan74.satena.scenes.entries2.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import org.threeten.bp.LocalDate

class Memorial15ViewModel(
    private val repository: EntriesRepository
) : EntriesFragmentViewModel() {
    private val years = (2005 .. LocalDate.now().year).toList()

    override val tabCount: Int = years.size
    override fun getTabTitle(context: Context, position: Int) : String =
        context.getString(R.string.entries_tab_15th, years[position])

    class Factory(
        private val repository : EntriesRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            Memorial15ViewModel(repository) as T
    }
}
