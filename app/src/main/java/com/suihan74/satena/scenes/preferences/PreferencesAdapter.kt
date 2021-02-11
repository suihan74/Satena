package com.suihan74.satena.scenes.preferences

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemGeneralButtonBinding
import com.suihan74.satena.databinding.ListviewItemGeneralSectionBinding
import com.suihan74.utilities.extensions.alsoAs

class PreferencesAdapter(
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<PreferencesAdapter.Item, PreferencesAdapter.ViewHolder>(
    DiffCallback()
) {
    override fun getItemViewType(position: Int): Int {
        return currentList[position].layoutId
    }

    override fun onCreateViewHolder(parent: ViewGroup, layoutId: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ViewDataBinding>(
            LayoutInflater.from(parent.context),
            layoutId,
            parent,
            false
        ).also {
            it.lifecycleOwner = lifecycleOwner
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        currentList[position].bind(holder.binding)
    }

    // ------ //

    class ViewHolder(val binding : ViewDataBinding) : RecyclerView.ViewHolder(binding.root)

    // ------ //

    interface Item {
        val layoutId : Int
        fun bind(binding: ViewDataBinding)
        fun areItemsTheSame(old: Item, new: Item) : Boolean = old == new
        fun areContentsTheSame(old: Item, new: Item) : Boolean = old == new
    }

    class Section(@StringRes val textId: Int) : Item {
        override val layoutId : Int = R.layout.listview_item_general_section
        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemGeneralSectionBinding> {
                it.textId = textId
            }
        }
    }

    class Button(
        @StringRes val textId: Int,
        @StringRes val subTextId: Int? = null,
        var onClick : (()->Unit)? = null
    ) : Item {
        override val layoutId : Int = R.layout.listview_item_general_button
        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemGeneralButtonBinding> {
                it.textId = textId
                it.subTextId = subTextId
                it.root.setOnClickListener {
                    onClick?.invoke()
                }
            }
        }
    }

    // ------ //

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.areItemsTheSame(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.areContentsTheSame(oldItem, newItem)
        }
    }
}
