package com.suihan74.satena.scenes.browser.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemBrowserHistoryBinding
import com.suihan74.satena.models.browser.History
import com.suihan74.utilities.GeneralAdapter
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import com.suihan74.utilities.SectionViewHolder
import com.suihan74.utilities.extensions.alsoAs
import kotlinx.android.synthetic.main.listview_section_history.view.*
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

class HistoryAdapter(
    lifecycleOwner: LifecycleOwner
) : GeneralAdapter<History, ListviewItemBrowserHistoryBinding>(
    lifecycleOwner,
    R.layout.listview_item_browser_history,
    DiffCallback()
) {
    /** 日付表示のフォーマット */
    private val dateFormatter : DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("uuuu年MM月dd日")
    }

    override fun bind(model: History?, binding: ListviewItemBrowserHistoryBinding) {
        binding.site = model
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
            items.sortedByDescending { it.lastVisited }.forEach { item ->
                val itemDate = item.lastVisited.toLocalDate()
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
                    vh.itemView.text_view.text = date?.format(dateFormatter) ?: ""
                }
            }

            else -> super.onBindViewHolder(holder, position)
        }
    }

    // ------ //

    class DiffCallback : GeneralAdapter.DiffCallback<History>() {
        override fun areModelsTheSame(oldItem: History?, newItem: History?): Boolean =
            oldItem?.url == newItem?.url

        override fun areModelContentsTheSame(oldItem: History?, newItem: History?): Boolean =
            oldItem?.url == newItem?.url &&
            oldItem?.title == newItem?.title &&
            oldItem?.lastVisited == newItem?.lastVisited &&
            oldItem?.faviconUrl == newItem?.faviconUrl
    }
}
