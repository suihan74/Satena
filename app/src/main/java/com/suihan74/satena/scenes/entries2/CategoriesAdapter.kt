package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.GlideApp
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.utilities.ItemClickedListener
import kotlinx.android.synthetic.main.listview_item_categories.view.*

class CategoriesAdapter : ListAdapter<Category, CategoriesAdapter.ViewHolder>(DiffCallback()) {
    override fun getItemCount() = currentList.size

    /** 表示形式にあわせてアイテムの表示を変える */
    private var itemLayoutId : Int = R.layout.listview_item_categories

    /** 項目クリック時の挙動 */
    private var onItemClicked : ItemClickedListener<Category>? = null

    /** 項目クリック時の挙動をセット */
    fun setOnItemClickedListener(listener: ItemClickedListener<Category>?) {
        onItemClicked = listener
    }

    fun updateLayout(itemLayoutId: Int) {
        this.itemLayoutId = itemLayoutId
        val items = currentList.toList()
        submitList(emptyList()) {
            submitList(items)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(itemLayoutId, parent, false)
        val holder = ViewHolder(inflate)

        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            val category = currentList[position]
            onItemClicked?.invoke(category)
        }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.category = currentList[position]
    }

    /** DiffCallback for ListAdapter */
    private class DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.ordinal == newItem.ordinal
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }

    /** ViewHolder for Items */
    inner class ViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        var category : Category? = null
            internal set(value) {
                field = value

                if (value != null) {
                    val context = view.context

                    view.category_text.text = context.getString(value.textId)

                    val drawable = context.getDrawable(value.iconId)

                    GlideApp.with(view)
                        .load(drawable)
                        .into(view.category_icon)
                }
            }
    }
}
