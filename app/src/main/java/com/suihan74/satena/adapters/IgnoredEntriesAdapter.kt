package com.suihan74.satena.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import com.suihan74.satena.R
import com.suihan74.satena.models.IgnoredEntry

open class IgnoredEntriesAdapter(private val ignoredEntries: List<IgnoredEntry>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val states = RecyclerState.makeStatesWithFooter(ignoredEntries.reversed())

    class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        private val mMode = root.findViewById<TextView>(R.id.mode_text)
        private val mQuery = root.findViewById<TextView>(R.id.query_text)

        var entry : IgnoredEntry? = null
            internal set(value) {
                field = value
                if (value == null) { return }

                mMode.text = "[${value.type.name}]"
                mQuery.text = value.query
            }
    }

    fun addItem(entry: IgnoredEntry) {
        states.add(0, RecyclerState(RecyclerType.BODY, entry))
        notifyItemInserted(0)
    }

    fun removeItem(entry: IgnoredEntry) {
        val position = states.indexOfFirst { it.type == RecyclerType.BODY && it.body == entry }
        states.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder =
        when (RecyclerType.fromInt(viewType)) {
            RecyclerType.BODY -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_ignored_entries, parent, false)
                ViewHolder(inflate).apply {
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
}
