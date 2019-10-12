package com.suihan74.satena.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R

open class PostTagsListAdapter(
    private val tags: List<String>
) : RecyclerView.Adapter<PostTagsListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LayoutInflater.from(parent.context)
        .inflate(R.layout.listview_item_tags, parent, false)
        .let {
            ViewHolder(it).apply {
                itemView.setOnClickListener {
                    onItemClicked(tag!!)
                }

                itemView.setOnLongClickListener {
                    return@setOnLongClickListener onItemLongClicked(tag!!)
                }
            }
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tag = tags[position]
    }

    override fun getItemCount() = tags.size

    open fun onItemClicked(tag: String) {}
    open fun onItemLongClicked(tag: String) : Boolean = false

    class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        private val text = root.findViewById<TextView>(R.id.text)

        var tag: String? = null
            set(value) {
                field = value
                text.text = value
            }
    }
}
