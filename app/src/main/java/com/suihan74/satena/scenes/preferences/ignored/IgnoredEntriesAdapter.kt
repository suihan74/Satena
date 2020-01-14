package com.suihan74.satena.scenes.preferences.ignored

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType

open class IgnoredEntriesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var states = emptyList<RecyclerState<IgnoredEntry>>()

    fun setItem(entries: List<IgnoredEntry>) {
        val newEntries = RecyclerState.makeStatesWithFooter(entries)

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder =
        when (RecyclerType.fromInt(viewType)) {
            RecyclerType.BODY -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_ignored_entries, parent, false)
                ViewHolder(
                    inflate
                ).apply {
                    itemView.setOnClickListener {
                        onItemClicked(this.entry!!)
                    }

                    itemView.setOnLongClickListener {
                        onItemLongClicked(this.entry!!)
                    }
                }
            }

            else -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.footer_recycler_view, parent, false)
                FooterViewHolder(inflate)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (RecyclerType.fromInt(holder.itemViewType)) {
            RecyclerType.BODY -> {
                (holder as ViewHolder).entry = states[position].body!!
            }

            else -> {}
        }
    }

    override fun getItemCount(): Int = states.size

    override fun getItemViewType(position: Int): Int = states[position].type.int

    open fun onItemClicked(entry: IgnoredEntry) {}
    open fun onItemLongClicked(entry: IgnoredEntry) : Boolean = true

    class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        private val mMode = root.findViewById<TextView>(R.id.mode_text)
        private val mQuery = root.findViewById<TextView>(R.id.query_text)

        var entry : IgnoredEntry? = null
            internal set(value) {
                field = value
                if (value == null) { return }

                mMode.text = String.format("[%s]", value.type.name)
                mQuery.text = value.query
            }
    }
}
