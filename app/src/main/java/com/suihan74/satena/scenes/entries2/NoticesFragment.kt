package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.putEnum

class NoticesFragment : EntriesTabFragmentBase() {
    companion object {
        fun createInstance(fragmentViewModelKey: String) = NoticesFragment()
            .apply {
            arguments = Bundle().apply {
                putString(ARG_FRAGMENT_VIEW_MODEL_KEY, fragmentViewModelKey)
                putEnum(ARG_CATEGORY, Category.Notices)
                putInt(ARG_TAB_POSITION, 0)
            }
        }
    }

    override fun initializeRecyclerView(
        entriesList: RecyclerView,
        swipeLayout: SwipeRefreshLayout
    ) {
        val context = requireContext()

        // エントリリスト用のアダプタ
        val noticesAdapter = NoticesAdapter().apply {
            setOnItemClickedListener { entry ->
                //MenuDialog.act(entry, activityViewModel.entryClickedAction, childFragmentManager, DIALOG_ENTRY_MENU)
            }

            setOnItemLongClickedListener { entry ->
                //MenuDialog.act(entry, activityViewModel.entryLongClickedAction, childFragmentManager, DIALOG_ENTRY_MENU)
                true
            }
        }

        // エントリリストの設定
        entriesList.apply {
            adapter = noticesAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(
                DividerItemDecorator(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.recycler_view_item_divider
                    )!!
                )
            )
        }

        // 引っ張って更新
        swipeLayout.apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(context.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(context.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                viewModel.refresh(onErrorRefreshEntries).invokeOnCompletion {
                    this.isRefreshing = false
                }
            }
        }
    }
}
