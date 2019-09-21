package com.suihan74.satena.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R

open class TagsAdapter(
    private var tags : List<String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        private val tagTextView = view.findViewById<TextView>(R.id.text)!!
        var tag : String
            get() = tagTextView.text.toString()
            internal set (value) {
                tagTextView.text = value
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_tags, parent, false)
        val holder = ViewHolder(inflate)

        holder.itemView.setOnClickListener {
            onItemClicked(holder.tag)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClicked(holder.tag)
        }

        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).apply {
            tag = tags[position]
        }
    }

    fun setTags(tags : List<String>) {
        this.tags = tags
        notifyDataSetChanged()
    }

    override fun getItemCount() = tags.size

    open fun onItemClicked(tag : String) {}
    open fun onItemLongClicked(tag : String) : Boolean = true
}
