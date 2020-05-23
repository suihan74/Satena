package com.suihan74.satena.scenes.preferences.userTag

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType

open class UserTagsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var states = emptyList<RecyclerState<TagAndUsers>>()

    fun setItems(tags: List<TagAndUsers>) {
        val newStates = RecyclerState.makeStatesWithFooter(tags.reversed())
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = states.size
            override fun getNewListSize() = newStates.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = states[oldItemPosition]
                val new = newStates[newItemPosition]
                return old.type == new.type && old.body?.userTag?.id == new.body?.userTag?.id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = states[oldItemPosition]
                val new = newStates[newItemPosition]
                return old.type == new.type &&
                        old.body?.userTag?.id == new.body?.userTag?.id &&
                        old.body?.userTag?.name == new.body?.userTag?.name &&
                        old.body?.users?.size == new.body?.users?.size
            }
        })
        states = newStates
        result.dispatchUpdatesTo(this)
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
