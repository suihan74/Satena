package com.suihan74.satena.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.BookmarkResult
import com.suihan74.satena.R

open class EntryCommentsAdapter(
    private val comments: List<BookmarkResult>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        private val icon = root.findViewById<ImageView>(R.id.icon)!!
        private val user = root.findViewById<TextView>(R.id.user_name)!!
        private val comment = root.findViewById<TextView>(R.id.comment)!!

        private var _value : BookmarkResult? = null
        var value : BookmarkResult
            get() = _value!!
            internal set(v) {
                _value = v

                user.text = v.user
                comment.text = v.commentRaw
                Glide.with(root)
                    .load(v.userIconUrl)
                    .into(icon)
            }
    }

    override fun getItemCount(): Int = comments.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_comment, parent, false)
        val holder = ViewHolder(inflate)

        holder.itemView.setOnClickListener {
            onItemClicked(holder.value)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClicked(holder.value)
        }

        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolder
        holder.value = comments[position]
    }

    open fun onItemClicked(item: BookmarkResult) {}
    open fun onItemLongClicked(item: BookmarkResult) : Boolean = true
}
