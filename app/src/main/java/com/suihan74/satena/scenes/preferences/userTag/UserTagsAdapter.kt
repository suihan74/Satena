package com.suihan74.satena.scenes.preferences.userTag

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.databinding.FooterRecyclerViewBinding
import com.suihan74.satena.databinding.ListviewItemUserTagsBinding
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (RecyclerType.fromId(viewType)) {
            RecyclerType.BODY -> {
                ViewHolder(
                    ListviewItemUserTagsBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                ).apply {
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

            else -> FooterViewHolder(FooterRecyclerViewBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (RecyclerType.fromId(holder.itemViewType)) {
            RecyclerType.BODY -> {
                (holder as ViewHolder).tag = states[position].body!!
            }

            else -> {}
        }
    }

    override fun getItemCount(): Int = states.size

    override fun getItemViewType(position: Int): Int = states[position].type.id

    open fun onItemClicked(tag: TagAndUsers) {}
    open fun onItemLongClicked(tag: TagAndUsers) : Boolean = true

    class ViewHolder(val binding: ListviewItemUserTagsBinding) : RecyclerView.ViewHolder(binding.root) {
        var tag : TagAndUsers? = null
            internal set(value) {
                field = value
                if (value == null) { return }

                binding.tagName.text = value.userTag.name
                binding.usersCount.text = String.format("%d users", value.users.size)
            }
    }
}
