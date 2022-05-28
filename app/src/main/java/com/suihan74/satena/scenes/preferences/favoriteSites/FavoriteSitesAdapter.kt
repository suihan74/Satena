package com.suihan74.satena.scenes.preferences.favoriteSites

import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemFavoriteSitesBinding
import com.suihan74.satena.models.favoriteSite.FavoriteSiteAndFavicon
import com.suihan74.utilities.GeneralAdapter
import com.suihan74.utilities.extensions.alsoAs

class FavoriteSitesAdapter(
    lifecycleOwner: LifecycleOwner
) : GeneralAdapter<FavoriteSiteAndFavicon, ListviewItemFavoriteSitesBinding>(
    lifecycleOwner,
    R.layout.listview_item_favorite_sites,
    DiffCallback()
) {
    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("items")
        fun setFavoriteSites(view: RecyclerView, items: List<FavoriteSiteAndFavicon>?) {
            if (items == null) return
            view.adapter.alsoAs<FavoriteSitesAdapter> { adapter ->
                adapter.setItems(items)
            }
        }
    }

    // ------ //

    override fun bind(model: FavoriteSiteAndFavicon?, binding: ListviewItemFavoriteSitesBinding) {
        binding.site = model
    }

    // ------ //

    class DiffCallback : GeneralAdapter.DiffCallback<FavoriteSiteAndFavicon>() {
        override fun areModelsTheSame(oldItem: FavoriteSiteAndFavicon?, newItem: FavoriteSiteAndFavicon?): Boolean =
            oldItem?.site?.id == newItem?.site?.id

        override fun areModelContentsTheSame(oldItem: FavoriteSiteAndFavicon?, newItem: FavoriteSiteAndFavicon?): Boolean =
            true == oldItem?.site?.same(newItem?.site) && oldItem.faviconInfo?.filename == newItem?.faviconInfo?.filename
    }
}
