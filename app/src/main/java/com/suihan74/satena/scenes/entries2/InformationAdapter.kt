package com.suihan74.satena.scenes.entries2

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ObservableBoolean
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.MaintenanceEntry
import com.suihan74.satena.databinding.FooterRecyclerViewBinding
import com.suihan74.satena.databinding.ListviewItemMaintenanceEntries2Binding
import com.suihan74.utilities.*

/** 項目と内容の表示状態 */
typealias InformationItem = Pair<MaintenanceEntry, ObservableBoolean>

class InformationAdapter : ListAdapter<RecyclerState<InformationItem>, RecyclerView.ViewHolder>(DiffCallback()) {
    /** クリック時の挙動 */
    private var onItemClicked : ItemClickedListener<MaintenanceEntry>? = null
    /** 長押し時の挙動 */
    private var onItemLongClicked : ItemLongClickedListener<MaintenanceEntry>? = null

    /** 項目クリック時の挙動をセットする */
    fun setOnItemClickedListener(listener: ItemClickedListener<MaintenanceEntry>?) {
        onItemClicked = listener
    }

    /** 項目長押し時の挙動をセットする */
    fun setOnItemLongClickedListener(listener: ItemLongClickedListener<MaintenanceEntry>?) {
        onItemLongClicked = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            RecyclerType.BODY.id -> {
                val binding = ListviewItemMaintenanceEntries2Binding.inflate(inflater, parent, false).also {
                    it.body.movementMethod = LinkMovementMethod.getInstance()
                }
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
                val item = currentList[position].body

                holder.item = item
                holder.itemView.apply {
                    setOnClickListener {
                        if (item != null) {
                            onItemClicked?.invoke(item.first)
                            item.second.set(!item.second.get())
                        }
                    }
                    setOnLongClickListener {
                        if (item != null) onItemLongClicked?.invoke(item.first) ?: false
                        else true
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int) =
        currentList[position].type.id

    /** このメソッドを使ってセットする */
    fun submitInformation(items: List<MaintenanceEntry>?, commitCallback: (()->Any?)? = null) {
        val newList : List<RecyclerState<InformationItem>> =
            if (items.isNullOrEmpty()) emptyList()
            else RecyclerState.makeStatesWithFooter(items.map { it to ObservableBoolean(false) })

        submitList(newList) {
            commitCallback?.invoke()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecyclerState<InformationItem>>() {
        override fun areItemsTheSame(
            oldItem: RecyclerState<InformationItem>,
            newItem: RecyclerState<InformationItem>
        ) = oldItem.type == newItem.type && oldItem.body?.first?.id == newItem.body?.first?.id

        override fun areContentsTheSame(
            oldItem: RecyclerState<InformationItem>,
            newItem: RecyclerState<InformationItem>
        ) = oldItem.type == newItem.type && oldItem.body?.first?.timestampUpdated == newItem.body?.first?.timestampUpdated
    }

    inner class ViewHolder(
        private val binding: ListviewItemMaintenanceEntries2Binding
    ) : RecyclerView.ViewHolder(binding.root) {
        var item: InformationItem? = null
            set(value) {
                field = value
                binding.item = value?.first
                binding.bodyVisibility = value?.second
            }
    }
}
