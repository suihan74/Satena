package com.suihan74.satena.scenes.preferences.favoriteSites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemFavoriteSitesBinding
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.Listener
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType

class FavoriteSitesAdapter(
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<RecyclerState<FavoriteSite>, RecyclerView.ViewHolder>(DiffCallback())
{
    var onClickItem: Listener<FavoriteSite>? = null

    var onLongClickItem: Listener<FavoriteSite>? = null

    fun setOnClickItemListener(listener: Listener<FavoriteSite>?) {
        onClickItem = listener
    }

    fun setOnLongLickItemListener(listener: Listener<FavoriteSite>?) {
        onLongClickItem = listener
    }

    // ------ //

    fun setItems(items: List<FavoriteSite>?, callback: Runnable? = null) {
        submitList(
            items?.let { RecyclerState.makeStatesWithFooter(it) },
            callback
        )
    }

    override fun getItemViewType(position: Int) = currentList[position].type.int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            RecyclerType.BODY.int -> {
                val binding = DataBindingUtil.inflate<ListviewItemFavoriteSitesBinding>(
                    inflater,
                    R.layout.listview_item_favorite_sites, parent, false
                ).also {
                    it.lifecycleOwner = lifecycleOwner
                }

                ViewHolder(binding).also { vh ->
                    vh.itemView.setOnClickListener {
                        val site = vh.binding.site ?: return@setOnClickListener
                        onClickItem?.invoke(site)
                    }

                    vh.itemView.setOnLongClickListener {
                        val site = vh.binding.site ?: return@setOnLongClickListener false
                        onLongClickItem?.invoke(site)
                        onLongClickItem != null
                    }
                }
            }

            RecyclerType.FOOTER.int -> FooterViewHolder(
                inflater.inflate(R.layout.footer_recycler_view, parent, false)
            )

            else -> throw NotImplementedError()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            RecyclerType.BODY.int -> {
                holder as ViewHolder
                val site = currentList[position].body
                holder.binding.site = site
            }
        }
    }

    // ------ //

    class DiffCallback : DiffUtil.ItemCallback<RecyclerState<FavoriteSite>>() {
        override fun areItemsTheSame(
            oldItem: RecyclerState<FavoriteSite>,
            newItem: RecyclerState<FavoriteSite>
        ): Boolean =
            oldItem.type == newItem.type && oldItem.body?.url == newItem.body?.url

        override fun areContentsTheSame(
            oldItem: RecyclerState<FavoriteSite>,
            newItem: RecyclerState<FavoriteSite>
        ): Boolean =
            oldItem.type == newItem.type && true == oldItem.body?.same(newItem.body)
    }

    // ------ //

    class ViewHolder(
        val binding: ListviewItemFavoriteSitesBinding
    ) : RecyclerView.ViewHolder(binding.root) {
    }
}
