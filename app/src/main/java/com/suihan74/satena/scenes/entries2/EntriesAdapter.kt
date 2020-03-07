package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemEntries2Binding
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.LoadableFooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import kotlinx.android.synthetic.main.listview_item_entries2.view.*

class EntriesAdapter : ListAdapter<RecyclerState<Entry>, RecyclerView.ViewHolder>(DiffCallback()) {
    private var onItemClicked : ((Entry)->Unit)? = null
    private var onItemLongClicked : ((Entry)->Boolean)? = null

    /** 項目クリック時の挙動をセットする */
    fun setOnItemClickedListener(listener: ((Entry)->Unit)?) {
        onItemClicked = listener
    }

    /** 項目長押し時の挙動をセットする */
    fun setOnItemLongClickedListener(listener: ((Entry)->Boolean)?) {
        onItemLongClicked = listener
    }

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
                val entry = currentList[position].body

                holder.entry = entry
                holder.itemView.apply {
                    setOnClickListener {
                        if (entry != null) {
                            onItemClicked?.invoke(entry)
                        }
                    }
                    setOnLongClickListener {
                        if (entry != null) onItemLongClicked?.invoke(entry) ?: false
                        else true
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int) =
        currentList[position].type.int

    fun submitEntries(items: List<Entry>?) {
        val newList : List<RecyclerState<Entry>> =
            if (items == null) emptyList()
            else RecyclerState.makeStatesWithFooter(items)
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

    class ViewHolder(
        private val binding: ListviewItemEntries2Binding
    ) : RecyclerView.ViewHolder(binding.root) {
        var entry: Entry? = null
            set(value) {
                field = value
                binding.entry = value
            }

        init {
            binding.root.comments_list.apply {
                adapter = BookmarkCommentsAdapter()
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(
                    DividerItemDecorator(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.recycler_view_item_divider
                        )!!
                    )
                )
            }
        }
    }
}
