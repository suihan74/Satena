package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.utilities.getThemeColor
import kotlinx.android.synthetic.main.listview_item_categories.view.*


open class CategoriesAdapter : ListAdapter<Category, CategoriesAdapter.ViewHolder>(DiffCallback()) {
    override fun getItemCount() = currentList.size

    open fun onItemClicked(category : Category) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_categories, parent, false)
        val holder = ViewHolder(inflate)

        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            val category = currentList[position]
            onItemClicked(category)
        }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.category = currentList[position]
    }

    class DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.ordinal == newItem.ordinal
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        var category : Category? = null
            internal set(value) {
                field = value

                if (value != null) {
                    val context = view.context

                    view.category_text.text = context.getString(value.textId)

                    val drawable = context.getDrawable(value.iconId)?.apply {
                        setTint(context.getThemeColor(R.attr.textColor))
                    }

                    Glide.with(view)
                        .load(drawable)
                        .into(view.category_icon)
                }
            }
    }
}
