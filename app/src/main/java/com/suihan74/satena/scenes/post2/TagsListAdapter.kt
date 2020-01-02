package com.suihan74.satena.scenes.post2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import kotlinx.android.synthetic.main.listview_item_tags.view.*

open class TagsListAdapter : RecyclerView.Adapter<TagsListAdapter.ViewHolder>() {

    private var tags = emptyList<String>()

    open fun onItemClicked(tag: String) {}
    open fun onItemLongClicked(tag: String) : Boolean = false

    /** タグリストを更新 */
    fun setTags(newTags: List<String>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getNewListSize() = newTags.size
            override fun getOldListSize() = tags.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItemPosition == newItemPosition

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                tags[oldItemPosition] == newTags[newItemPosition]
        })
        tags = newTags

        diff.dispatchUpdatesTo(this)
    }

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

    class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
        var tag: String? = null
            set(value) {
                field = value
                root.text.text = value
            }
    }
}
