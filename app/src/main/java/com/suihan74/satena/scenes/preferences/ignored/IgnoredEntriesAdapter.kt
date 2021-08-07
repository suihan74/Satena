package com.suihan74.satena.scenes.preferences.ignored

import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemIgnoredEntriesBinding
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.utilities.GeneralAdapter
import com.suihan74.utilities.extensions.alsoAs

class IgnoredEntriesAdapter(lifecycleOwner: LifecycleOwner) : GeneralAdapter<IgnoredEntry, ListviewItemIgnoredEntriesBinding>(
    lifecycleOwner,
    R.layout.listview_item_ignored_entries,
    DiffCallback()
) {
    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("ignoredEntries")
        fun setIgnoredEntries(view: RecyclerView, items: List<IgnoredEntry>?) {
            if (items == null) return
            view.adapter.alsoAs<IgnoredEntriesAdapter> { adapter ->
                if (items.isEmpty()) adapter.setItems(null)
                else adapter.setItems(items)
            }
        }
    }

    // ------ //

    override fun bind(model: IgnoredEntry?, binding: ListviewItemIgnoredEntriesBinding) {
        binding.entry = model
    }

    // ------ //

    class DiffCallback : GeneralAdapter.DiffCallback<IgnoredEntry>() {
        override fun areModelsTheSame(
            oldItem: IgnoredEntry?,
            newItem: IgnoredEntry?
        ): Boolean {
            return oldItem == newItem
        }

        override fun areModelContentsTheSame(
            oldItem: IgnoredEntry?,
            newItem: IgnoredEntry?
        ): Boolean {
            return oldItem == newItem
        }
    }
}
