package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FooterRecyclerViewBinding
import com.suihan74.satena.databinding.ListviewItemNotices2Binding
import com.suihan74.utilities.*

class NoticesAdapter : ListAdapter<RecyclerState<Notice>, RecyclerView.ViewHolder>(DiffCallback()) {
    /** エントリクリック時の挙動 */
    private var onItemClicked : ItemClickedListener<Notice>? = null
    /** エントリ長押し時の挙動 */
    private var onItemLongClicked : ItemLongClickedListener<Notice>? = null

    /** 項目クリック時の挙動をセットする */
    fun setOnItemClickedListener(listener: ItemClickedListener<Notice>?) {
        onItemClicked = listener
    }

    /** 項目長押し時の挙動をセットする */
    fun setOnItemLongClickedListener(listener: ItemLongClickedListener<Notice>?) {
        onItemLongClicked = listener
    }

    /** 複数回クリックリスナが呼ばれるのを防ぐ */
    private var itemClicked = false

    fun onResume() {
        itemClicked = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            RecyclerType.BODY.id -> {
                val binding = DataBindingUtil.inflate<ListviewItemNotices2Binding>(
                    inflater,
                    R.layout.listview_item_notices2, parent, false
                )
                ViewHolder(binding)
            }

            RecyclerType.FOOTER.id -> FooterViewHolder(
                FooterRecyclerViewBinding.inflate(inflater, parent, false)
            )

            else -> throw NotImplementedError()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            RecyclerType.BODY.id -> {
                holder as ViewHolder
                val notice = currentList[position].body

                holder.notice = notice
                holder.itemView.apply {
                    setOnClickListener {
                        if (notice != null && !itemClicked) {
                            itemClicked = true
                            onItemClicked?.invoke(notice)
                        }
                    }
                    setOnLongClickListener {
                        if (notice != null) onItemLongClicked?.invoke(notice) ?: false
                        else true
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int) =
        currentList[position].type.id

    /** エントリはこのメソッドを使ってセットする */
    fun submitNotices(items: List<Notice>?, commitCallback: (()->Any?)? = null) {
        val newList : List<RecyclerState<Notice>> =
            if (items.isNullOrEmpty()) emptyList()
            else RecyclerState.makeStatesWithFooter(items)

        submitList(newList) {
            commitCallback?.invoke()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecyclerState<Notice>>() {
        override fun areItemsTheSame(
            oldItem: RecyclerState<Notice>,
            newItem: RecyclerState<Notice>
        ) = oldItem.type == newItem.type && oldItem.body?.created == newItem.body?.created

        override fun areContentsTheSame(
            oldItem: RecyclerState<Notice>,
            newItem: RecyclerState<Notice>
        ) = oldItem.type == newItem.type &&
                oldItem.body?.created == newItem.body?.created &&
                oldItem.body?.modified == newItem.body?.modified
    }

    inner class ViewHolder(
        private val binding: ListviewItemNotices2Binding
    ) : RecyclerView.ViewHolder(binding.root) {
        var notice: Notice? = null
            set(value) {
                field = value
                binding.notice = value
            }
    }
}
