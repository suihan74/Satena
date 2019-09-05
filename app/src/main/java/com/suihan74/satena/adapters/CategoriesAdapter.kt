package com.suihan74.satena.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.suihan74.HatenaLib.Category
import com.suihan74.satena.R


open class CategoriesAdapter(
    private var categories: Array<Category>
) : RecyclerView.Adapter<CategoriesAdapter.ViewHolder>() {

    fun setCategories(categories: Array<Category>) {
        this.categories = categories
        notifyDataSetChanged()
    }

    class ViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.category_text)!!
        private val icon  = view.findViewById<ImageView>(R.id.category_icon)!!

        var category : Category? = null
            internal set(value) {
                field = value

                if (value != null) {
                    val context = view.context
                    val name = value.name.toLowerCase()

                    val textId = context.resources.getIdentifier("category_$name", "string", context.packageName)
                    if (textId != 0x0) {
                        title.text = context.getString(textId)
                    }

                    val drawableId = context.resources.getIdentifier("ic_category_$name", "drawable", context.packageName)
                    if (drawableId != 0x0) {
                        val requestOptions = RequestOptions()
                            .placeholder(drawableId)

                        Glide.with(view)
                            .load("")
                            .apply(requestOptions)
                            .into(icon)
                    }
                }
            }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_categories, parent, false)
        val holder = ViewHolder(inflate)

        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            val category = categories[position]
            onItemClicked(category)
        }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.category = categories[position]
    }

    override fun getItemCount() = categories.size

    open fun onItemClicked(category : Category) {
    }
}
