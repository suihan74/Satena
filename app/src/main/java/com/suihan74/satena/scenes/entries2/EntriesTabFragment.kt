package com.suihan74.satena.scenes.entries2

import android.content.Intent
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.entries2.dialog.EntryMenuDialog
import com.suihan74.satena.scenes.entries2.dialog.EntryMenuDialogListeners
import com.suihan74.utilities.*

class EntriesTabFragment : EntriesTabFragmentBase() {
    companion object {
        fun createInstance(fragmentViewModelKey: String, category: Category, tabPosition: Int = 0) = EntriesTabFragment().withArguments {
            putString(ARG_FRAGMENT_VIEW_MODEL_KEY, fragmentViewModelKey)
            putEnum(ARG_CATEGORY, category)
            putInt(ARG_TAB_POSITION, tabPosition)
        }

        const val DIALOG_ENTRY_MENU = "EntriesTabFragment.DIALOG_ENTRY_MENU"
    }

    override fun initializeRecyclerView(entriesList: RecyclerView, swipeLayout: SwipeRefreshLayout) {
        val context = requireContext()

        // エントリリスト用のアダプタ
        val entriesAdapter = EntriesAdapter(viewLifecycleOwner).apply {
            // メニューアクション実行後に画面表示を更新する
            val listeners = EntryMenuDialogListeners().apply {
                onIgnoredEntry = { _ ->
                    (activity as? EntriesActivity)?.refreshLists()
                }
                onDeletedBookmark = { entry ->
                    (activity as? EntriesActivity)?.removeBookmark(entry)
                }
                onPostedBookmark = { entry, bookmarkResult ->
                    (activity as? EntriesActivity)?.updateBookmark(entry, bookmarkResult)
                }
            }

            setOnItemClickedListener { entry ->
                EntryMenuDialog.act(
                    context,
                    entry,
                    activityViewModel.entryClickedAction,
                    listeners,
                    childFragmentManager,
                    DIALOG_ENTRY_MENU
                )
            }

            if (activityViewModel.entryMultipleClickedAction == TapEntryAction.NOTHING) {
                setOnItemMultipleClickedListener(null)
            }
            else {
                setOnItemMultipleClickedListener { entry, _ ->
                    EntryMenuDialog.act(
                        context,
                        entry,
                        activityViewModel.entryMultipleClickedAction,
                        listeners,
                        childFragmentManager,
                        DIALOG_ENTRY_MENU
                    )
                }
            }

            setOnItemLongClickedListener { entry ->
                EntryMenuDialog.act(
                    context,
                    entry,
                    activityViewModel.entryLongClickedAction,
                    listeners,
                    childFragmentManager,
                    DIALOG_ENTRY_MENU
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
                entriesAdapter.setOnItemsSubmittedListener { list ->
                    if (list != null) {
                        this.isRefreshing = false
                        entriesList.scrollToPosition(0)
                        entriesAdapter.setOnItemsSubmittedListener(null)
                    }
                }
                viewModel.refresh(onErrorRefreshEntries).invokeOnCompletion { e ->
                    if (e != null) {
                        this.isRefreshing = false
                        entriesAdapter.setOnItemsSubmittedListener(null)
                    }
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
            layoutManager = LinearLayoutManager(context)
        }

        // 初回ロード完了後の処理
        entriesAdapter.setOnItemsSubmittedListener { list ->
            if (list != null) {
                entriesList.addOnScrollListener(scrollingUpdater)
                entriesAdapter.setOnItemsSubmittedListener(null)
            }
        }

        parentViewModel?.connectToTab(
            requireActivity(),
            entriesAdapter,
            viewModel,
            onErrorRefreshEntries
        )
    }
}

