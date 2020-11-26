package com.suihan74.satena.scenes.post

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.databinding.ListviewItemTagsBinding
import com.suihan74.utilities.Listener
import com.suihan74.utilities.Switcher

class TagsListAdapter : RecyclerView.Adapter<TagsListAdapter.ViewHolder>() {

    private var tags = emptyList<String>()

    private var onItemClicked : Listener<String>? = null
    private var onItemLongClicked : Listener<String>? = null
    private var onItemTouch : Switcher<MotionEvent>? = null

    fun setOnItemClickedListener(l: Listener<String>?) {
        onItemClicked = l
    }

    fun setOnItemLongClickedListener(l: Listener<String>?) {
        onItemLongClicked = l
    }

    fun setOnItemTouchListener(l: Switcher<MotionEvent>?) {
        onItemTouch = l
    }

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ViewHolder {
        val binding = ListviewItemTagsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding).apply {
            itemView.setOnClickListener {
                val tag = tag ?: return@setOnClickListener
                onItemClicked?.invoke(tag)
            }

            itemView.setOnLongClickListener {
                val tag = tag ?: return@setOnLongClickListener false
                onItemLongClicked?.invoke(tag)
                return@setOnLongClickListener onItemLongClicked != null
            }

            @Suppress("ClickableViewAccessibility")
            itemView.setOnTouchListener { _, motionEvent ->
                if (motionEvent == null || onItemTouch == null) {
                    return@setOnTouchListener false
                }
                else {
                    return@setOnTouchListener onItemTouch?.invoke(motionEvent) ?: false
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tag = tags[position]
    }

    override fun getItemCount() = tags.size

    class ViewHolder(private val binding: ListviewItemTagsBinding) : RecyclerView.ViewHolder(binding.root) {
        var tag: String? = null
            set(value) {
                field = value
                binding.text.text = value
            }
    }
}
