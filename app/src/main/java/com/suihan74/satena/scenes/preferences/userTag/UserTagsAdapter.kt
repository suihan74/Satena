package com.suihan74.satena.scenes.preferences.userTag

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType

open class UserTagsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val states = RecyclerState.makeStatesWithFooter(emptyList<TagAndUsers>())

    fun setItems(tags: List<TagAndUsers>) {
        states.clear()
        states.addAll(0, tags.map { RecyclerState(RecyclerType.BODY, it) })
        notifyDataSetChanged()
    }

    fun addItem(tag: TagAndUsers) {
        val position = states.size - 1
        states.add(position, RecyclerState(RecyclerType.BODY, tag))
        notifyItemInserted(position)
    }

    fun removeItem(tag: TagAndUsers) {
        val position = states.indexOfFirst { it.type == RecyclerType.BODY && it.body?.userTag?.id == tag.userTag.id }
        if (position >= 0) {
            states.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateItem(tag: TagAndUsers) {
        val position = states.indexOfFirst { it.type == RecyclerType.BODY && it.body?.userTag?.id == tag.userTag.id }
        if (position >= 0) {
            states[position].body = tag
            notifyItemChanged(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder =
        when (RecyclerType.fromInt(viewType)) {
            RecyclerType.BODY -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_user_tags, parent, false)
                ViewHolder(inflate).apply {
                    itemView.setOnClickListener {
                        val tag = this.tag
                        if (tag != null) {
                            onItemClicked(tag)
                        }
                    }

                    itemView.setOnLongClickListener {
                        val tag = this.tag
                        if (tag != null) {
                            onItemLongClicked(tag)
                        }
                        return@setOnLongClickListener true
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
                (holder as ViewHolder).tag = states[position].body!!
            }

            else -> {}
        }
    }

    override fun getItemCount(): Int = states.size

    override fun getItemViewType(position: Int): Int = states[position].type.int

    open fun onItemClicked(tag: TagAndUsers) {}
    open fun onItemLongClicked(tag: TagAndUsers) : Boolean = true

    class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        private val mName = root.findViewById<TextView>(R.id.tag_name)
        private val mCounter = root.findViewById<TextView>(R.id.users_count)

        var tag : TagAndUsers? = null
            internal set(value) {
                field = value
                if (value == null) { return }

                mName.text = value.userTag.name
                mCounter.text = String.format("%d users", value.users.size)
            }
    }
}
