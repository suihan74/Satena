package com.suihan74.satena.scenes.preferences.entries

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemPrefsEntriesDefaultTabBinding
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesTabType
import com.suihan74.utilities.GeneralAdapter
import com.suihan74.utilities.RecyclerState

data class EntriesDefaultTabSetting(
    val category: Category,
    val tab: LiveData<EntriesTabType>
)

// ------ //

class EntriesDefaultTabsAdapter(
    private val lifecycleOwner: LifecycleOwner
) : GeneralAdapter<EntriesDefaultTabSetting, ListviewItemPrefsEntriesDefaultTabBinding>(
    lifecycleOwner,
    R.layout.listview_item_prefs_entries_default_tab,
    DiffCallback()
) {
    override fun bind(
        model: EntriesDefaultTabSetting?,
        binding: ListviewItemPrefsEntriesDefaultTabBinding
    ) {
        binding.data = model
    }

    override fun setItems(items: List<EntriesDefaultTabSetting>?, callback: Runnable?) {
        submitList(
            items?.let {
                // 新しく追加した項目をリストの上側にする
                RecyclerState.makeStatesWithFooter(it)
            },
            callback
        )
    }

    // ------ //

    class DiffCallback : GeneralAdapter.DiffCallback<EntriesDefaultTabSetting>() {
        override fun areModelsTheSame(
            oldItem: EntriesDefaultTabSetting?,
            newItem: EntriesDefaultTabSetting?
        ) = oldItem?.category == newItem?.category

        override fun areModelContentsTheSame(
            oldItem: EntriesDefaultTabSetting?,
            newItem: EntriesDefaultTabSetting?
        ) = oldItem == newItem
    }
}
