package com.suihan74.satena.scenes.browser

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemBrowserBackStackBinding
import com.suihan74.satena.models.browser.HistoryPage
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
    GeneralAdapter<HistoryPage, ListviewItemBrowserBackStackBinding>(
        lifecycleOwner,
        R.layout.listview_item_browser_back_stack,
        DiffCallback()
    )
{
    override fun bind(model: HistoryPage?, binding: ListviewItemBrowserBackStackBinding) {
        binding.vm = viewModel
        binding.item = model
    }

    override fun setItems(items: List<HistoryPage>?, callback: Runnable?) {
        submitList(
            items?.let {
                // 新しく追加した項目をリストの上側にする
                it.asReversed().map { item ->
                    RecyclerState(RecyclerType.BODY, item)
                }
            },
            callback
        )
    }

    // ------ //

    class DiffCallback : GeneralAdapter.DiffCallback<HistoryPage>() {
        override fun areModelsTheSame(oldItem: HistoryPage?, newItem: HistoryPage?): Boolean {
            return oldItem?.url == newItem?.url
        }

        override fun areModelContentsTheSame(oldItem: HistoryPage?, newItem: HistoryPage?): Boolean {
            return oldItem?.url == newItem?.url
                    && oldItem?.title == newItem?.title
                    && oldItem?.faviconUrl == newItem?.faviconUrl
        }
    }

    // ------ //

    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("backStack")
        fun setBackStack(recyclerView: RecyclerView, items: List<HistoryPage>?) {
            recyclerView.adapter.alsoAs<BackStackAdapter> { adapter ->
                adapter.setItems(items)
            }
        }

        /** 現在表示中ページの「戻る/進む」履歴項目背景色を装飾する */
        @JvmStatic
        @BindingAdapter("currentUrl", "itemUrl")
        fun setBackStackItemBackground(view: View, currentUrl: String?, itemUrl: String?) {
            val context = view.context
            view.setBackgroundColor(
                if (currentUrl == itemUrl) {
                    context.getThemeColor(R.attr.browserBackStackCurrentPageBackground)
                }
                else {
                    context.getColor(android.R.color.transparent)
                }
            )
        }
    }
}

