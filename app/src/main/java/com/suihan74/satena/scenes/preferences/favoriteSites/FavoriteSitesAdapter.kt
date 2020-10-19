package com.suihan74.satena.scenes.preferences.favoriteSites

import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemFavoriteSitesBinding
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.utilities.GeneralAdapter
import com.suihan74.utilities.extensions.alsoAs

class FavoriteSitesAdapter(
    lifecycleOwner: LifecycleOwner
) : GeneralAdapter<FavoriteSite, ListviewItemFavoriteSitesBinding>(
    lifecycleOwner,
    R.layout.listview_item_favorite_sites,
    DiffCallback()
) {
    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("items")
        fun setFavoriteSites(view: RecyclerView, items: List<FavoriteSite>?) {
            if (items == null) return
            view.adapter.alsoAs<FavoriteSitesAdapter> { adapter ->
                adapter.setItems(items)
            }
        }
    }

    // ------ //

    override fun bind(model: FavoriteSite?, binding: ListviewItemFavoriteSitesBinding) {
        binding.site = model
    }

    // ------ //

    class DiffCallback : GeneralAdapter.DiffCallback<FavoriteSite>() {
        override fun areModelsTheSame(oldItem: FavoriteSite?, newItem: FavoriteSite?): Boolean =
            oldItem?.url == newItem?.url

        override fun areModelContentsTheSame(oldItem: FavoriteSite?, newItem: FavoriteSite?): Boolean =
            true == oldItem?.same(newItem)
    }
}
