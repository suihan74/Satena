package com.suihan74.satena.scenes.bookmarks.information

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.databinding.ListviewItemTagsBinding
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.alsoAs

class TagsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var tags : List<String> = emptyList()

    // ------ //

    private var onItemClicked : Listener<String>? = null

    private var onItemLongClicked : Listener<String>? = null

    fun setOnItemClickedListener(l : Listener<String>?) {
        onItemClicked = l
    }

    fun setOnItemLongClicked(l : Listener<String>?) {
        onItemLongClicked = l
    }

    // ------ //

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListviewItemTagsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding).also { vh ->
            vh.itemView.setOnClickListener {
                onItemClicked?.invoke(vh.tag)
            }

            vh.itemView.setOnLongClickListener {
                onItemLongClicked?.invoke(vh.tag) != null
            }
        }
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

    // ------ //

    /** タグアイテム */
    class ViewHolder(val binding : ListviewItemTagsBinding) : RecyclerView.ViewHolder(binding.root) {
        var tag : String
            get() = binding.text.text.toString()
            internal set (value) {
                binding.text.text = value
            }
    }

    // ------ //

    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("bookmarksEntryTags")
        fun setTags(view: RecyclerView, tags: List<Pair<String, Int>>?) {
            view.adapter.alsoAs<TagsAdapter> { adapter ->
                tags?.map { t -> t.first }?.take(10)?.let {
                    adapter.setTags(it)
                }
            }
        }
    }
}
