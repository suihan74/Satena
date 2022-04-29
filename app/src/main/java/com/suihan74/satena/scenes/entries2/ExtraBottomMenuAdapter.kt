package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.GlideApp
import com.suihan74.satena.databinding.ListviewItemExtraBottomMenuBinding
import com.suihan74.utilities.Listener

class ExtraBottomMenuAdapter : ListAdapter<UserBottomItem, ExtraBottomMenuAdapter.ViewHolder>(DiffCallback()) {

    private var onClickListener : Listener<UserBottomItem>? = null

    fun setOnClickListener(listener: Listener<UserBottomItem>?) {
        onClickListener = listener
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
