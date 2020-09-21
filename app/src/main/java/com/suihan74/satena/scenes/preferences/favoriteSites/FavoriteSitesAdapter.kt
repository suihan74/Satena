package com.suihan74.satena.scenes.preferences.favoriteSites

import androidx.lifecycle.LifecycleOwner
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemFavoriteSitesBinding
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.utilities.GeneralAdapter

class FavoriteSitesAdapter(
    lifecycleOwner: LifecycleOwner
) : GeneralAdapter<FavoriteSite, ListviewItemFavoriteSitesBinding>(
    lifecycleOwner,
    R.layout.listview_item_favorite_sites,
    DiffCallback()
) {
    override fun bind(model: FavoriteSite?, binding: ListviewItemFavoriteSitesBinding) {
        binding.site = model
    }

    class DiffCallback : GeneralAdapter.DiffCallback<FavoriteSite>() {
        override fun areModelsTheSame(oldItem: FavoriteSite?, newItem: FavoriteSite?): Boolean =
            oldItem?.url == newItem?.url

        override fun areModelContentsTheSame(oldItem: FavoriteSite?, newItem: FavoriteSite?): Boolean =
            true == oldItem?.same(newItem)
    }
}
