package com.suihan74.satena.scenes.entries.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.HatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.EntriesHistoryKey
import com.suihan74.satena.scenes.entries.SingleTabEntriesFragmentBase
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.async

class HistoryFragment : SingleTabEntriesFragmentBase() {
    companion object {
        fun createInstance() = HistoryFragment()
    }

    override val title = SatenaApplication.instance.getString(R.string.category_history)

    override val currentCategory = Category.History

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!
        refreshEntries()
        return root
    }

    override fun refreshEntries() =
        super.refreshEntries("履歴の取得失敗") {
            async {
                val prefs = SafeSharedPreferences.create<EntriesHistoryKey>(context)
                val size = prefs.getInt(EntriesHistoryKey.MAX_SIZE)
                val entries = prefs.get<List<Entry>>(EntriesHistoryKey.ENTRIES)
                return@async entries.reversed().take(size)
            }
        }
}
