package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.suihan74.satena.GlideApp
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemCategoriesBinding
import com.suihan74.satena.databinding.ListviewItemCategoriesGridBinding
import com.suihan74.satena.models.Category
import com.suihan74.utilities.ItemClickedListener
import com.suihan74.utilities.extensions.alsoAs

class CategoriesAdapter :
    ListAdapter<Category, CategoriesAdapter.ViewHolder>(DiffCallback())
{
    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("src")
        fun setCategories(view: RecyclerView, categories: Array<Category>?) {
            if (categories == null) return
            view.adapter.alsoAs<CategoriesAdapter> { adapter ->
                adapter.submitList(categories.toList())
            }
        }
    }

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
        val inflater = LayoutInflater.from(parent.context)
        val binding = when (itemLayoutId) {
            R.layout.listview_item_categories ->
                ListviewItemCategoriesBinding.inflate(
                    inflater,
                    parent,
                    false
                )

            R.layout.listview_item_categories_grid ->
                ListviewItemCategoriesGridBinding.inflate(
                    inflater,
                    parent,
                    false
                )

            else -> throw IllegalArgumentException()
        }
        val holder = ViewHolder(binding)

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

    private interface Binder {
        val categoryText : TextView
        val categoryIcon : ImageView
    }

    private class BinderForLinear(val binding: ListviewItemCategoriesBinding) : Binder {
        override val categoryText = binding.categoryText
        override val categoryIcon = binding.categoryIcon
    }

    private class BinderForGrid(val binding: ListviewItemCategoriesGridBinding) : Binder {
        override val categoryText = binding.categoryText
        override val categoryIcon = binding.categoryIcon
    }

    /** ViewHolder for Items */
    inner class ViewHolder(val binding : ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        private val binder : Binder = when(binding) {
            is ListviewItemCategoriesBinding -> BinderForLinear(binding)
            is ListviewItemCategoriesGridBinding -> BinderForGrid(binding)
            else -> throw IllegalArgumentException()
        }

        var category : Category? = null
            internal set(value) {
                field = value

                if (value != null) {
                    val context = binding.root.context

                    binder.categoryText.text = context.getString(value.textId)

                    val drawable = ContextCompat.getDrawable(context, value.iconId)

                    GlideApp.with(context)
                        .load(drawable)
                        .into(binder.categoryIcon)
                }
            }
    }
}
