package com.suihan74.satena.scenes.preferences.ignored

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.databinding.FooterRecyclerViewBinding
import com.suihan74.satena.databinding.ListviewItemIgnoredEntriesBinding
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType

open class IgnoredEntriesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var states = emptyList<RecyclerState<IgnoredEntry>>()

    fun setItem(entries: List<IgnoredEntry>) {
        val newEntries = RecyclerState.makeStatesWithFooter(entries.reversed())

        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = states.size
            override fun getNewListSize() = newEntries.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) : Boolean {
                val old = states[oldItemPosition]
                val new = newEntries[newItemPosition]
                return old.type == new.type && old.body?.id == new.body?.id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = states[oldItemPosition]
                val new = newEntries[newItemPosition]
                return old.type == new.type &&
                        old.body?.type == new.body?.type &&
                        old.body?.query == new.body?.query &&
                        old.body?.target == new.body?.target
            }
        })
        states = newEntries

        result.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (RecyclerType.fromId(viewType)) {
            RecyclerType.BODY -> {
                ViewHolder(
                    ListviewItemIgnoredEntriesBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                ).apply {
                    itemView.setOnClickListener {
                        onItemClicked(this.entry!!)
                    }

                    itemView.setOnLongClickListener {
                        onItemLongClicked(this.entry!!)
                    }
                }
            }

            else -> FooterViewHolder(FooterRecyclerViewBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (RecyclerType.fromId(holder.itemViewType)) {
            RecyclerType.BODY -> {
                (holder as ViewHolder).entry = states[position].body!!
            }

            else -> {}
        }
    }

    override fun getItemCount(): Int = states.size

    override fun getItemViewType(position: Int): Int = states[position].type.id

    open fun onItemClicked(entry: IgnoredEntry) {}
    open fun onItemLongClicked(entry: IgnoredEntry) : Boolean = true

    class ViewHolder(val binding: ListviewItemIgnoredEntriesBinding) : RecyclerView.ViewHolder(binding.root) {
        var entry : IgnoredEntry? = null
            internal set(value) {
                field = value
                if (value == null) { return }

                binding.modeText.text = String.format("[%s]", value.type.name)
                binding.queryText.text = value.query
            }
    }
}
