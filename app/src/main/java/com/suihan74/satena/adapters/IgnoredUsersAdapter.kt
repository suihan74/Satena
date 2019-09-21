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

open class IgnoredUsersAdapter(
    private var _users : List<String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var states : List<String> = _users
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun setUsers(u: List<String>) {
        _users = u
        states = if (searchText.isEmpty()) u else u.filter { it.contains(searchText) }
    }

    fun isEmpty() = _users.isEmpty()

    class ViewHolder(private val view : View) : RecyclerView.ViewHolder(view) {
        private val userIcon = view.findViewById<ImageView>(R.id.user_icon)!!
        private val userName = view.findViewById<TextView>(R.id.user_name)!!
        var user : String
            get() = userName.text.toString()
            internal set (value) {
                userName.text = value
                val iconUrl = HatenaClient.getUserIconUrl(value)

                Glide.with(view)
                    .load(iconUrl)
                    .into(userIcon)
            }
    }

    fun removeUser(user: String) {
        val position = states.indexOf(user)
        _users = _users.filterNot { it == user }.toList()
        states = states.filterNot { it == user }.toList()
        notifyItemRemoved(position)
    }

    var searchText = ""
        set(value) {
            field = value
            states = if (value.isEmpty()) _users else _users.filter { it.contains(value) }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_ignored_users, parent, false)
        val holder = ViewHolder(inflate)

        holder.itemView.setOnClickListener {
            onItemClicked(holder.user)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClicked(holder.user)
        }

        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).apply {
            user = states[position]
        }
    }

    override fun getItemCount() = states.size

    open fun onItemClicked(user : String) {}
    open fun onItemLongClicked(user : String) : Boolean = true
}
