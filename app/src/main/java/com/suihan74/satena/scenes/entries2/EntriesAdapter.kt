package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemEntries2Binding
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.LoadableFooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import kotlinx.android.synthetic.main.footer_recycler_view_loadable.view.*

class EntriesAdapter : ListAdapter<RecyclerState<Entry>, RecyclerView.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            RecyclerType.BODY.int -> {
                val binding = DataBindingUtil.inflate<ListviewItemEntries2Binding>(
                    inflater,
                    R.layout.listview_item_entries2, parent, false
                )
                ViewHolder(binding)
            }

            RecyclerType.FOOTER.int ->
                LoadableFooterViewHolder(
                    inflater.inflate(R.layout.footer_recycler_view_loadable, parent, false)
                )

            else -> throw NotImplementedError()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            RecyclerType.BODY.int -> {
                holder as ViewHolder
                holder.entry = currentList[position].body
            }
        }
    }

    override fun getItemViewType(position: Int) =
        currentList[position].type.int

    fun submitEntries(items: List<Entry>) {
        val newList = RecyclerState.makeStatesWithFooter(items)
        submitList(newList)
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecyclerState<Entry>>() {
        override fun areItemsTheSame(
            oldItem: RecyclerState<Entry>,
            newItem: RecyclerState<Entry>
        ) = oldItem.type == newItem.type && oldItem.body?.id == newItem.body?.id

        override fun areContentsTheSame(
            oldItem: RecyclerState<Entry>,
            newItem: RecyclerState<Entry>
        ) = oldItem.type == newItem.type &&
                oldItem.body?.id == newItem.body?.id &&
                oldItem.body?.count == newItem.body?.count &&
                oldItem.body?.title == newItem.body?.title &&
                oldItem.body?.bookmarkedData == newItem.body?.bookmarkedData
    }

    class ViewHolder(private val binding: ListviewItemEntries2Binding) : RecyclerView.ViewHolder(binding.root) {
        var entry: Entry? = null
            set(value) {
                field = value
                binding.entry = value
            }
    }
}
