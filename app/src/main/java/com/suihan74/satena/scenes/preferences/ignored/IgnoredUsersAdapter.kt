package com.suihan74.satena.scenes.preferences.ignored

import androidx.lifecycle.LifecycleOwner
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemIgnoredUsersBinding
import com.suihan74.utilities.GeneralAdapter

class IgnoredUsersAdapter(
    lifecycleOwner: LifecycleOwner
) : GeneralAdapter<String, ListviewItemIgnoredUsersBinding>(
    lifecycleOwner,
    R.layout.listview_item_ignored_users,
    DiffCallback()
) {
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
