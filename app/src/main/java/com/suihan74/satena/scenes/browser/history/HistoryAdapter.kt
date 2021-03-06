package com.suihan74.satena.scenes.browser.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemBrowserHistoryBinding
import com.suihan74.satena.databinding.ListviewSectionHistoryBinding
import com.suihan74.satena.models.browser.History
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.alsoAs
import org.threeten.bp.LocalDate

class HistoryAdapter(
    val viewModel: HistoryViewModel,
    lifecycleOwner: LifecycleOwner
) : GeneralAdapter<History, ListviewItemBrowserHistoryBinding>(
    lifecycleOwner,
    R.layout.listview_item_browser_history,
    DiffCallback()
) {
    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("items")
        fun setHistory(view: RecyclerView, items: List<RecyclerState<History>>?) {
            items?.let {
                view.adapter.alsoAs<HistoryAdapter> { adapter ->
                    adapter.submitList(it)
                }
            }
        }
    }

    // ------ //

    /** 日付指定で履歴を削除する */
    private var onClearByDate : Listener<LocalDate>? = null

    /** 日付指定で履歴を削除する */
    fun setOnClearByDateListener(l: Listener<LocalDate>?) {
        onClearByDate = l
    }

    override fun bind(model: History?, binding: ListviewItemBrowserHistoryBinding) {
        binding.history = model
        binding.vm = viewModel
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (RecyclerType.fromId(viewType)) {
            // 日付ごとの区切りを表示する
            RecyclerType.SECTION -> {
                val binding = ListviewSectionHistoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SectionViewHolder(binding)
            }

            else -> super.onCreateViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = currentList[position]
        when (item.type) {
            RecyclerType.SECTION -> {
                holder.alsoAs<SectionViewHolder> { vh ->
                    vh.binding.alsoAs<ListviewSectionHistoryBinding> { binding ->
                        val date = item.extra as? LocalDate
                        binding.textView.text = date?.format(viewModel.dateFormatter) ?: ""
                        binding.clearButton.setOnClickListener {
                            if (date != null) {
                                onClearByDate?.invoke(date)
                            }
                        }
                    }
                }
            }

            else -> super.onBindViewHolder(holder, position)
        }
    }

    // ------ //

    class DiffCallback : GeneralAdapter.DiffCallback<History>() {
        override fun areModelsTheSame(oldItem: History?, newItem: History?): Boolean =
            oldItem?.log?.id == newItem?.log?.id

        override fun areModelContentsTheSame(oldItem: History?, newItem: History?): Boolean =
            oldItem?.page == newItem?.page
    }
}
