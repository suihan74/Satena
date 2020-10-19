package com.suihan74.satena.scenes.browser.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemBrowserHistoryBinding
import com.suihan74.satena.models.browser.History
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.alsoAs
import kotlinx.android.synthetic.main.listview_section_history.view.*
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
        fun setHistory(view: RecyclerView, items: List<History>?) {
            if (items == null) return
            view.adapter.alsoAs<HistoryAdapter> { adapter ->
                adapter.setItems(items)
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

    override fun setItems(items: List<History>?, callback: Runnable?) {
        if (items.isNullOrEmpty()) {
            submitList(null, callback)
            return
        }

        // 日付ごとに区切りを表示する
        @OptIn(ExperimentalStdlibApi::class)
        val states = buildList {
            var currentDate: LocalDate? = null
            items.sortedByDescending { it.log.visitedAt }.forEach { item ->
                val itemDate = item.log.visitedAt.toLocalDate()
                if (currentDate != itemDate) {
                    currentDate = itemDate
                    add(RecyclerState(
                        type = RecyclerType.SECTION,
                        extra = itemDate
                    ))
                }

                add(RecyclerState(
                    type = RecyclerType.BODY,
                    body = item
                ))
            }
            add(RecyclerState(RecyclerType.FOOTER))
            Unit
        }

        submitList(states, callback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (RecyclerType.fromInt(viewType)) {
            // 日付ごとの区切りを表示する
            RecyclerType.SECTION -> {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.listview_section_history, parent, false)
                SectionViewHolder(view)
            }

            else -> super.onCreateViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = currentList[position]
        when (item.type) {
            RecyclerType.SECTION -> {
                holder.alsoAs<SectionViewHolder> { vh ->
                    val date = item.extra as? LocalDate
                    vh.itemView.text_view?.text = date?.format(viewModel.dateFormatter) ?: ""
                    vh.itemView.clear_button?.setOnClickListener {
                        if (date != null) {
                            onClearByDate?.invoke(date)
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
            oldItem?.page?.url == newItem?.page?.url &&
            oldItem?.page?.title == newItem?.page?.title &&
            oldItem?.log?.visitedAt == newItem?.log?.visitedAt &&
            oldItem?.page?.faviconUrl == newItem?.page?.faviconUrl
    }
}
