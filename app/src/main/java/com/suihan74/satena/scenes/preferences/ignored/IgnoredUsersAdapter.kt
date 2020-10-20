package com.suihan74.satena.scenes.preferences.ignored

import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemIgnoredUsersBinding
import com.suihan74.utilities.GeneralAdapter
import com.suihan74.utilities.extensions.alsoAs

class IgnoredUsersAdapter(
    lifecycleOwner: LifecycleOwner
) : GeneralAdapter<String, ListviewItemIgnoredUsersBinding>(
    lifecycleOwner,
    R.layout.listview_item_ignored_users,
    DiffCallback()
) {
    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("ignoredUsers")
        fun setIgnoredUsers(view: RecyclerView, items: List<String>?) {
            if (items == null) return
            view.adapter.alsoAs<IgnoredUsersAdapter> { adapter ->
                if (items.isEmpty()) adapter.setItems(null)
                else adapter.setItems(items)
            }
        }
    }

    // ------ //

    override fun bind(model: String?, binding: ListviewItemIgnoredUsersBinding) {
        val user = model ?: ""
        binding.user = user
        binding.iconUrl = HatenaClient.getUserIconUrl(user)
    }

    // ------ //

    class DiffCallback : GeneralAdapter.DiffCallback<String>() {
        override fun areModelsTheSame(
            oldItem: String?,
            newItem: String?
        ): Boolean {
            return oldItem == newItem
        }

        override fun areModelContentsTheSame(
            oldItem: String?,
            newItem: String?
        ): Boolean {
            return oldItem == newItem
        }
    }
}
