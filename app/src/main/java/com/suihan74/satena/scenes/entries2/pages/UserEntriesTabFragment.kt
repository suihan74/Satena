package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesAdapter
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.utilities.RecyclerViewScrollingUpdater
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.observerForOnlyUpdates
import com.suihan74.utilities.extensions.putEnum
import com.suihan74.utilities.extensions.withArguments

class UserEntriesTabFragment : EntriesTabFragmentBase() {
    companion object {
        fun createInstance(fragmentViewModelKey: String, user: String) = UserEntriesTabFragment().withArguments {
            putString(ARG_FRAGMENT_VIEW_MODEL_KEY, fragmentViewModelKey)
            putEnum(ARG_CATEGORY, Category.User)
            putString(ARG_USER, user)
        }

        private const val ARG_USER = "ARG_USER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ユーザー名をセット
        viewModel.user = requireArguments().getString(ARG_USER)!!
    }

    override fun initializeRecyclerView(
        entriesList: RecyclerView,
        swipeLayout: SwipeRefreshLayout
    ) {
        val context = requireContext()

        // エントリリスト用のアダプタ
        val entriesAdapter = EntriesAdapter(viewLifecycleOwner)

        // 引っ張って更新
        swipeLayout.setOnRefreshListener {
            viewModel.reloadLists(
                onError = onErrorRefreshEntries,
                onFinally = { swipeLayout.isRefreshing = false }
            )
        }

        // スクロールで追加ロード
        val scrollingUpdater = RecyclerViewScrollingUpdater {
            entriesAdapter.showProgressBar()
            viewModel.loadAdditional(
                onFinally = {
                    entriesAdapter.hideProgressBar()
                    loadCompleted()
                },
                onError = { e ->
                    context.showToast(R.string.msg_get_entry_failed)
                    Log.e("loadAdditional", Log.getStackTraceString(e))
                }
            )
        }

        // エントリリストの設定
        entriesList.run {
            adapter = entriesAdapter
            addOnScrollListener(scrollingUpdater)
            layoutManager = LinearLayoutManager(context)
        }

        // タグの変更を監視
        parentViewModel?.tag?.observe(viewLifecycleOwner, observerForOnlyUpdates {
            viewModel.tag = it
            entriesAdapter.submitEntries(null) {
                viewModel.reloadLists(onError = onErrorRefreshEntries)
            }
        })
    }
}
