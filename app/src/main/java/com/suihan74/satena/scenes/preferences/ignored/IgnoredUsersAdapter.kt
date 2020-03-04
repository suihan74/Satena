package com.suihan74.satena.scenes.preferences.ignored

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import kotlinx.android.synthetic.main.listview_item_ignored_users.view.*

open class IgnoredUsersAdapter(
    private var _users : List<String>
) : ListAdapter<String, RecyclerView.ViewHolder>(DiffCallback()) {
    private class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }

    fun setUsers(u: List<String>) {
        _users = u
        submitList(
            if (searchText.isEmpty()) u
            else u.filter { it.contains(searchText) }
        )
    }

    fun isEmpty() = _users.isEmpty()

    class ViewHolder(private val view : View) : RecyclerView.ViewHolder(view) {
        var user : String
            get() = view.user_name.text.toString()
            internal set (value) {
                view.user_name.text = value
                val iconUrl = HatenaClient.getUserIconUrl(value)

                Glide.with(view)
                    .load(iconUrl)
                    .into(view.user_icon)
            }
    }

    fun removeUser(user: String) {
        _users = _users.filterNot { it == user }.toList()
        submitList(currentList.filterNot { it == user }.toList())
    }

    var searchText = ""
        set(value) {
            field = value
            submitList(
                if (value.isEmpty()) _users
                else _users.filter { it.contains(value) }
            )
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_ignored_users, parent, false)
        val holder =
            ViewHolder(inflate)

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
            user = currentList[position]
        }
    }

    override fun getItemCount() = currentList.size

    open fun onItemClicked(user : String) {}
    open fun onItemLongClicked(user : String) : Boolean = true
}
