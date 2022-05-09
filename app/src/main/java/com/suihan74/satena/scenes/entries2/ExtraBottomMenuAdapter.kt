package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.GlideApp
import com.suihan74.satena.databinding.ListviewItemExtraBottomMenuBinding
import com.suihan74.utilities.ItemClickedListener
import com.suihan74.utilities.ItemLongClickedListener

class ExtraBottomMenuAdapter : ListAdapter<UserBottomItem, ExtraBottomMenuAdapter.ViewHolder>(DiffCallback()) {

    private var onClickListener : ItemClickedListener<UserBottomItem>? = null
    private var onLongClickListener : ItemLongClickedListener<UserBottomItem>? = null

    fun setOnClickListener(listener: ItemClickedListener<UserBottomItem>?) {
        onClickListener = listener
    }

    fun setOnLongClickListener(listener: ItemLongClickedListener<UserBottomItem>?) {
        onLongClickListener = listener
    }

    // ------ //

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ListviewItemExtraBottomMenuBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        currentList[position].let { item ->
            holder.binding.text.setText(item.textId)
            GlideApp.with(holder.binding.icon)
                .load(item.iconId)
                .into(holder.binding.icon)
            holder.binding.root.setOnClickListener {
                onClickListener?.invoke(item)
            }
            holder.binding.root.setOnLongClickListener {
                onLongClickListener?.invoke(item) == true
            }
        }
    }

    // ------ //

    class ViewHolder(val binding: ListviewItemExtraBottomMenuBinding) : RecyclerView.ViewHolder(binding.root) {
    }

    // ------ //

    class DiffCallback : DiffUtil.ItemCallback<UserBottomItem>() {
        override fun areItemsTheSame(oldItem: UserBottomItem, newItem: UserBottomItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: UserBottomItem, newItem: UserBottomItem): Boolean =
            oldItem == newItem
    }
}
