package com.suihan74.satena.scenes.browser.history

import androidx.lifecycle.LifecycleOwner
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemBrowserHistoryBinding
import com.suihan74.satena.models.browser.History
import com.suihan74.utilities.GeneralAdapter

class HistoryAdapter(
    lifecycleOwner: LifecycleOwner
) : GeneralAdapter<History, ListviewItemBrowserHistoryBinding>(
    lifecycleOwner,
    R.layout.listview_item_browser_history,
    DiffCallback()
) {
    override fun bind(model: History?, binding: ListviewItemBrowserHistoryBinding) {
        binding.site = model
    }

    class DiffCallback : GeneralAdapter.DiffCallback<History>() {
        override fun areModelsTheSame(oldItem: History?, newItem: History?): Boolean =
            oldItem?.url == newItem?.url

        override fun areModelContentsTheSame(oldItem: History?, newItem: History?): Boolean =
            oldItem?.url == newItem?.url &&
            oldItem?.title == newItem?.title &&
            oldItem?.lastVisited == newItem?.lastVisited &&
            oldItem?.faviconUrl == newItem?.faviconUrl
    }
}
