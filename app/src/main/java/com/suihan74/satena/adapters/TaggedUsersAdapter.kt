package com.suihan74.satena.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.models.TaggedUser
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType

open class TaggedUsersAdapter(users : Collection<TaggedUser>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val states = RecyclerState.makeStatesWithFooter(users.reversed())

    fun addItem(user: TaggedUser) {
        states.add(0, RecyclerState(RecyclerType.BODY, user))
        notifyItemInserted(0)
    }

    fun removeItem(user: TaggedUser) {
        val position = states.indexOfFirst { it.type == RecyclerType.BODY && it.body?.id == user.id }
        if (position >= 0) {
            states.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder =
        when (RecyclerType.fromInt(viewType)) {
            RecyclerType.BODY -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_ignored_users, parent, false)
                ViewHolder(inflate).apply {
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
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.footer_recycler_view, parent, false)
                FooterViewHolder(inflate)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (RecyclerType.fromInt(holder.itemViewType)) {
            RecyclerType.BODY -> {
                (holder as ViewHolder).user = states[position].body!!
            }

            else -> {}
        }
    }

    override fun getItemCount(): Int = states.size

    override fun getItemViewType(position: Int): Int = states[position].type.int

    open fun onItemClicked(user: TaggedUser) {}
    open fun onItemLongClicked(user: TaggedUser) : Boolean = true

    class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
        private val userIcon = root.findViewById<ImageView>(R.id.user_icon)
        private val userName = root.findViewById<TextView>(R.id.user_name)

        var user : TaggedUser? = null
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
