package com.suihan74.satena.scenes.entries2

import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.utilities.RecyclerViewScrollingUpdater
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.putEnum
import com.suihan74.utilities.extensions.withArguments

class EntriesTabFragment : EntriesTabFragmentBase() {
    companion object {
        fun createInstance(
            fragmentViewModelKey: String,
            category: Category,
            tabPosition: Int = 0
        ) = EntriesTabFragment().withArguments {
            putString(ARG_FRAGMENT_VIEW_MODEL_KEY, fragmentViewModelKey)
            putEnum(ARG_CATEGORY, category)
            putInt(ARG_TAB_POSITION, tabPosition)
        }
    }

    override fun initializeRecyclerView(entriesList: RecyclerView, swipeLayout: SwipeRefreshLayout) {
        val context = requireContext()

        // エントリリスト用のアダプタ
        val entriesAdapter = EntriesAdapter(viewLifecycleOwner)

        // 引っ張って更新
        swipeLayout.setOnRefreshListener {
            entriesAdapter.setOnItemsSubmittedListener { list ->
                if (list != null) {
                    swipeLayout.isRefreshing = false
                    entriesList.scrollToPosition(0)
                    entriesAdapter.setOnItemsSubmittedListener(null)
                }
            }
            lifecycleScope.launchWhenResumed {
                runCatching { viewModel.reloadLists() }
                    .onFailure {
                        onErrorRefreshEntries(it)
                        entriesAdapter.setOnItemsSubmittedListener(null)
                    }
                swipeLayout.isRefreshing = false
            }
        }

        // スクロールで追加ロード
        val scrollingUpdater = RecyclerViewScrollingUpdater {
            entriesAdapter.showProgressBar()
            lifecycleScope.launchWhenResumed {
                runCatching { viewModel.loadAdditional() }
                    .onFailure {
                        context.showToast(R.string.msg_get_entry_failed)
                        Log.e("loadAdditional", Log.getStackTraceString(it))
                    }
                entriesAdapter.hideProgressBar()
                loadCompleted()
            }
        }

        // エントリリストの設定
        entriesList.run {
            adapter = entriesAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // 初回ロード完了後の処理
        entriesAdapter.setOnItemsSubmittedListener { list ->
            if (list != null) {
                entriesList.addOnScrollListener(scrollingUpdater)
                entriesAdapter.setOnItemsSubmittedListener(null)
            }
        }

        parentViewModel?.connectToTab(this, entriesAdapter, viewModel) { onErrorRefreshEntries(it) }
    }
}

