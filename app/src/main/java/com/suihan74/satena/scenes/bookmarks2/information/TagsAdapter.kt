package com.suihan74.satena.scenes.bookmarks2.information

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R

open class TagsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var tags : List<String> = emptyList()

    open fun onItemClicked(tag : String) {}
    open fun onItemLongClicked(tag : String) : Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_tags, parent, false)
        val holder =
            ViewHolder(inflate)

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

    override fun getItemCount() = tags.size

    fun setTags(tags : List<String>) {
        val oldItems = this.tags
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldItems.size
            override fun getNewListSize() = tags.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = oldItems[oldItemPosition]
                val new = tags[newItemPosition]
                return old == new
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = oldItems[oldItemPosition]
                val new = tags[newItemPosition]
                return old == new
            }
        })
        this.tags = tags
        diff.dispatchUpdatesTo(this)
    }

    /** タグアイテム */
    class ViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        private val tagTextView = view.findViewById<TextView>(R.id.text)!!
        var tag : String
            get() = tagTextView.text.toString()
            internal set (value) {
                tagTextView.text = value
            }
    }
}
