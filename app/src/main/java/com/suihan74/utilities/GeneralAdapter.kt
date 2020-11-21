package com.suihan74.utilities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R

abstract class GeneralAdapter<ModelT, BindingT : ViewDataBinding>(
    private val lifecycleOwner : LifecycleOwner,
    @LayoutRes private val itemLayoutId : Int,
    diffCallback: DiffCallback<ModelT>
) : ListAdapter<RecyclerState<ModelT>, RecyclerView.ViewHolder>(diffCallback)
{
    private var onClickItem: Listener<BindingT>? = null

    private var onLongClickItem: Listener<BindingT>? = null

    fun setOnClickItemListener(listener: Listener<BindingT>?) {
        onClickItem = listener
    }

    fun setOnLongLickItemListener(listener: Listener<BindingT>?) {
        onLongClickItem = listener
    }

    // ------ //

    /** モデルをリストアイテムビューにバインドする */
    abstract fun bind(model: ModelT?, binding: BindingT)

    // ------ //

    open fun setItems(items: List<ModelT>?, callback: Runnable? = null) {
        submitList(
            items?.let {
                // 新しく追加した項目をリストの上側にする
                RecyclerState.makeStatesWithFooter(it.asReversed())
            },
            callback
        )
    }

    override fun getItemViewType(position: Int) = currentList[position].type.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            RecyclerType.BODY.id -> {
                val binding = DataBindingUtil.inflate<BindingT>(
                    inflater,
                    itemLayoutId, parent, false
                ).also {
                    it.lifecycleOwner = lifecycleOwner
                }

                ViewHolder(binding).also { vh ->
                    vh.itemView.setOnClickListener {
                        onClickItem?.invoke(vh.binding)
                    }

                    vh.itemView.setOnLongClickListener {
                        onLongClickItem?.invoke(vh.binding)
                        onLongClickItem != null
                    }
                }
            }

            RecyclerType.FOOTER.id -> FooterViewHolder(
                inflater.inflate(R.layout.footer_recycler_view, parent, false)
            )

            else -> throw NotImplementedError()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            RecyclerType.BODY.id -> {
                holder as ViewHolder<BindingT>
                bind(currentList[position].body, holder.binding)
            }
        }
    }

    // ------ //

    abstract class DiffCallback<ModelT> : DiffUtil.ItemCallback<RecyclerState<ModelT>>() {
        abstract fun areModelsTheSame(oldItem: ModelT?, newItem: ModelT?): Boolean
        abstract fun areModelContentsTheSame(oldItem: ModelT?, newItem: ModelT?): Boolean

        final override fun areItemsTheSame(
            oldItem: RecyclerState<ModelT>,
            newItem: RecyclerState<ModelT>
        ): Boolean =
            oldItem.type == newItem.type &&
                    (oldItem.type != RecyclerType.BODY || areModelsTheSame(oldItem.body, newItem.body))

        final override fun areContentsTheSame(
            oldItem: RecyclerState<ModelT>,
            newItem: RecyclerState<ModelT>
        ): Boolean =
            oldItem.type == newItem.type &&
                    (oldItem.type != RecyclerType.BODY || areModelContentsTheSame(oldItem.body, newItem.body))
    }

    // ------ //

    class ViewHolder<BindingT : ViewDataBinding>(
        val binding: BindingT
    ) : RecyclerView.ViewHolder(binding.root)
}
