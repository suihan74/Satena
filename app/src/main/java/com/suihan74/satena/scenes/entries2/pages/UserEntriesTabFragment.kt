package com.suihan74.satena.scenes.entries2.pages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.entries2.EntriesAdapter
import com.suihan74.satena.scenes.entries2.EntriesTabFragment
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.satena.scenes.entries2.dialog.EntryMenuDialog
import com.suihan74.utilities.*

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
        val entriesAdapter = EntriesAdapter(this).apply {
            setOnItemClickedListener { entry ->
                EntryMenuDialog.act(
                    context,
                    entry,
                    activityViewModel.entryClickedAction,
                    childFragmentManager,
                    EntriesTabFragment.DIALOG_ENTRY_MENU
                )
            }

            setOnItemLongClickedListener { entry ->
                EntryMenuDialog.act(
                    context,
                    entry,
                    activityViewModel.entryLongClickedAction,
                    childFragmentManager,
                    EntriesTabFragment.DIALOG_ENTRY_MENU
                )
                true
            }

            // コメント部分クリック時の挙動
            setOnCommentClickedListener { entry, bookmark ->
                val intent = Intent(context, BookmarksActivity::class.java).apply {
                    putObjectExtra(BookmarksActivity.EXTRA_ENTRY, entry)
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

        // タグの変更を監視
        var isTagInitialized = false
        val parentViewModel = parentViewModel!!
        parentViewModel.tag.observe(viewLifecycleOwner) {
            if (!isTagInitialized) {
                isTagInitialized = true
                return@observe
            }

            viewModel.tag = it
            entriesAdapter.submitEntries(null) {
                viewModel.refresh(onErrorRefreshEntries)
            }
        }
    }
}
