package com.suihan74.satena.scenes.preferences.ignored

import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemIgnoredEntries2Binding
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.utilities.GeneralAdapter
import com.suihan74.utilities.extensions.alsoAs

class IgnoredEntriesAdapter2(lifecycleOwner: LifecycleOwner) : GeneralAdapter<IgnoredEntry, ListviewItemIgnoredEntries2Binding>(
    lifecycleOwner,
    R.layout.listview_item_ignored_entries2,
    DiffCallback()
) {
    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("ignoredEntries")
        fun setIgnoredEntries(view: RecyclerView, items: List<IgnoredEntry>?) {
            if (items == null) return
            view.adapter.alsoAs<IgnoredEntriesAdapter2> { adapter ->
                if (items.isEmpty()) adapter.setItems(null)
                else adapter.setItems(items)
            }
        }
    }

    // ------ //

    override fun bind(model: IgnoredEntry?, binding: ListviewItemIgnoredEntries2Binding) {
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
