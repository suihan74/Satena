package com.suihan74.satena.scenes.browser

import android.view.View
import android.webkit.WebBackForwardList
import android.webkit.WebHistoryItem
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemBrowserBackStackBinding
import com.suihan74.utilities.GeneralAdapter
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getThemeColor

/** 「戻る/進む」履歴 */
class BackStackAdapter(
    private val viewModel : BrowserViewModel,
    lifecycleOwner : LifecycleOwner
) :
    GeneralAdapter<WebHistoryItem, ListviewItemBrowserBackStackBinding>(
        lifecycleOwner,
        R.layout.listview_item_browser_back_stack,
        DiffCallback()
    )
{
    override fun bind(model: WebHistoryItem?, binding: ListviewItemBrowserBackStackBinding) {
        binding.vm = viewModel
        binding.index = currentList.size - currentList.indexOfFirst { it.body == model } - 1
        binding.item = model
    }

    override fun setItems(items: List<WebHistoryItem>?, callback: Runnable?) {
        submitList(
            items?.asReversed()?.map { item ->
                RecyclerState(RecyclerType.BODY, item)
            },
            callback
        )
    }

    // ------ //

    class DiffCallback : GeneralAdapter.DiffCallback<WebHistoryItem>() {
        override fun areModelsTheSame(oldItem: WebHistoryItem?, newItem: WebHistoryItem?): Boolean {
            return oldItem?.originalUrl == newItem?.originalUrl
        }

        override fun areModelContentsTheSame(oldItem: WebHistoryItem?, newItem: WebHistoryItem?): Boolean {
            return oldItem?.url == newItem?.url
                    && oldItem?.title == newItem?.title
                    && oldItem?.originalUrl == newItem?.originalUrl
        }
    }

    // ------ //

    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("backStack")
        fun setBackStack(recyclerView: RecyclerView, items: WebBackForwardList?) {
            recyclerView.adapter.alsoAs<BackStackAdapter> { adapter ->
                val list = items?.let { items ->
                    (0 until items.size).map { items.getItemAtIndex(it) }
                } ?: emptyList()
                adapter.setItems(list)
            }
        }

        /** 現在表示中ページの「戻る/進む」履歴項目背景色を装飾する */
        @JvmStatic
        @BindingAdapter("list", "itemIdx")
        fun setBackStackItemBackground(view: View, list: WebBackForwardList?, itemIdx: Int?) {
            val context = view.context
            view.setBackgroundColor(
                if (list?.currentIndex == itemIdx) {
                    context.getThemeColor(R.attr.browserBackStackCurrentPageBackground)
                }
                else {
                    context.getColor(android.R.color.transparent)
                }
            )
        }
    }
}

