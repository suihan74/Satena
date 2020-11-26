package com.suihan74.satena.scenes.preferences.userTag

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FooterRecyclerViewBinding
import com.suihan74.satena.models.userTag.User
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType

open class TaggedUsersAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var states = emptyList<RecyclerState<User>>()

    fun setItems(users: List<User>) {
        val newStates = RecyclerState.makeStatesWithFooter(users.reversed())
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = states.size
            override fun getNewListSize() = newStates.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = states[oldItemPosition]
                val new = newStates[newItemPosition]
                return old.type == new.type && old.body?.id == new.body?.id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = states[oldItemPosition]
                val new = newStates[newItemPosition]
                return old.type == new.type &&
                        old.body?.id == new.body?.id &&
                        old.body?.name == new.body?.name
            }
        })
        states = newStates
        result.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder =
        when (RecyclerType.fromId(viewType)) {
            RecyclerType.BODY -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_ignored_users, parent, false)
                ViewHolder(inflate)
                    .apply {
                    itemView.setOnClickListener {
                        val user = this.user
                        if (user != null) {
                            onItemClicked(user)
                        }
                    }

                    itemView.setOnLongClickListener {
                        val user = this.user
                        if (user != null) {
                            onItemLongClicked(user)
                        }
                        return@setOnLongClickListener true
                    }
                }
            }

            else -> {
                val binding = FooterRecyclerViewBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                FooterViewHolder(binding)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (RecyclerType.fromId(holder.itemViewType)) {
            RecyclerType.BODY -> {
                (holder as ViewHolder).user = states[position].body!!
            }

            else -> {}
        }
    }

    override fun getItemCount(): Int = states.size

    override fun getItemViewType(position: Int): Int = states[position].type.id

    open fun onItemClicked(user: User) {}
    open fun onItemLongClicked(user: User) : Boolean = true

    class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
        private val userIcon = root.findViewById<ImageView>(R.id.user_icon)
        private val userName = root.findViewById<TextView>(R.id.user_name)

        var user : User? = null
            internal set(value) {
                field = value
                if (value == null) { return }

                val iconUrl = HatenaClient.getUserIconUrl(value.name)
                userName.text = value.name
                Glide.with(root)
                    .load(iconUrl)
                    .into(userIcon)
            }
    }
}
