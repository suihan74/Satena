package com.suihan74.satena.scenes.entries2.pages

import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.satena.scenes.entries2.InformationAdapter
import com.suihan74.utilities.extensions.putEnum
import com.suihan74.utilities.extensions.withArguments

class InformationTabFragment : EntriesTabFragmentBase() {
    companion object {
        fun createInstance(fragmentViewModelKey: String) = InformationTabFragment().withArguments {
            putString(ARG_FRAGMENT_VIEW_MODEL_KEY, fragmentViewModelKey)
            putEnum(ARG_CATEGORY, Category.Maintenance)
        }
    }

    override fun initializeRecyclerView(
        entriesList: RecyclerView,
        swipeLayout: SwipeRefreshLayout
    ) {
        val context = requireContext()

        // エントリリスト用のアダプタ
        val adapter = InformationAdapter()

        // エントリリストの設定
        entriesList.adapter = adapter

        // 引っ張って更新
        swipeLayout.setOnRefreshListener {
            viewModel.reloadLists(
                onError = onErrorRefreshEntries,
                onFinally = { swipeLayout.isRefreshing = false }
            )
        }
    }
}
