package com.suihan74.satena.scenes.entries2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.entries2.dialog.EntryMenuDialog
import com.suihan74.utilities.RecyclerViewScrollingUpdater
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.showToast

class EntriesTabFragment : EntriesTabFragmentBase() {
    companion object {
        fun createInstance(fragmentViewModelKey: String, category: Category, tabPosition: Int = 0) = EntriesTabFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_FRAGMENT_VIEW_MODEL_KEY, fragmentViewModelKey)
                putEnum(ARG_CATEGORY, category)
                putInt(ARG_TAB_POSITION, tabPosition)
            }
        }

        private const val DIALOG_ENTRY_MENU = "entry_menu_dialog"
    }

    override fun initializeRecyclerView(entriesList: RecyclerView, swipeLayout: SwipeRefreshLayout) {
        val context = requireContext()

        // エントリリスト用のアダプタ
        val entriesAdapter = EntriesAdapter().apply {
            setOnItemClickedListener { entry ->
                EntryMenuDialog.act(entry, activityViewModel.entryClickedAction, childFragmentManager, DIALOG_ENTRY_MENU)
            }

            setOnItemLongClickedListener { entry ->
                EntryMenuDialog.act(entry, activityViewModel.entryLongClickedAction, childFragmentManager, DIALOG_ENTRY_MENU)
                true
            }

            // コメント部分クリック時の挙動
            setOnCommentClickedListener { entry, bookmark ->
                val intent = Intent(context, BookmarksActivity::class.java).apply {
                    putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
                    putExtra(BookmarksActivity.EXTRA_TARGET_USER, bookmark.user)
                }
                startActivity(intent)
            }
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

        // スクロールで追加ロード
        val scrollingUpdater = RecyclerViewScrollingUpdater {
            entriesAdapter.showProgressBar()
            viewModel.loadAdditional(
                onFinally = { loadCompleted() },
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

        // Issueの変更を監視する
        // Issueの選択を監視している親のEntriesFragmentから状態をもらってくる
        val parentViewModelKey = requireArguments().getString(ARG_FRAGMENT_VIEW_MODEL_KEY)!!
        val parentViewModel = ViewModelProvider(requireActivity())[parentViewModelKey, EntriesFragmentViewModel::class.java]
        var isIssueInitialized = false
        parentViewModel.issue.observe(viewLifecycleOwner, Observer {
            if (!isIssueInitialized) {
                isIssueInitialized = true
                return@Observer
            }

            viewModel.issue = it
            // 一度クリアしておかないとスクロール位置が滅茶苦茶になる
            entriesAdapter.submitEntries(null) {
                viewModel.refresh(onErrorRefreshEntries)
            }
        })
    }
}

