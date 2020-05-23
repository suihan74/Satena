package com.suihan74.satena.scenes.entries2

import android.content.Intent
import android.util.Log
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.entries2.dialog.EntryMenuDialog
import com.suihan74.satena.scenes.entries2.dialog.EntryMenuDialogListeners
import com.suihan74.satena.scenes.entries2.pages.SearchEntriesViewModel
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
        val entriesAdapter = EntriesAdapter().apply {

            // TODO: 他のタブにも反映されるようにしたい
            val listeners = EntryMenuDialogListeners().apply {
                onIgnoredEntry = { _ ->
                    viewModel.filter()
                }
                onDeletedBookmark = { entry ->
                    viewModel.delete(entry)
                }
            }

            setOnItemClickedListener { entry ->
                EntryMenuDialog.act(entry, activityViewModel.entryClickedAction, listeners, childFragmentManager, DIALOG_ENTRY_MENU)
            }

            setOnItemLongClickedListener { entry ->
                EntryMenuDialog.act(entry, activityViewModel.entryLongClickedAction, listeners, childFragmentManager, DIALOG_ENTRY_MENU)
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
        var isIssueInitialized = false
        val parentViewModel = parentViewModel!!
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

        // Tagの変更を監視する
        var isTagInitialized = false
        parentViewModel.tag.observe(viewLifecycleOwner, Observer {
            if (!isTagInitialized) {
                isTagInitialized = true
                return@Observer
            }

            viewModel.tag = it
            entriesAdapter.submitEntries(null) {
                viewModel.refresh(onErrorRefreshEntries)
            }
        })

        // TODO: なんとかした方がよさそうというかクラス分けた方が良さそうな気もする
        // 以下はActivity生成と同時に遷移してくる関係上
        // isInitialized的なやつで初期化時ロードを避けるとロードできなくなってしまうので注意

        // SiteUrlを監視する
        parentViewModel.siteUrl.observe(viewLifecycleOwner, Observer {
            viewModel.siteUrl = it
            viewModel.refresh(onErrorRefreshEntries)
        })

        // 検索情報を監視する
        (parentViewModel as? SearchEntriesViewModel)?.let { parent ->
            parent.searchQuery.observe(viewLifecycleOwner, Observer {
                viewModel.searchQuery = it
            })
            parent.searchType.observe(viewLifecycleOwner, Observer {
                viewModel.searchType = it
                viewModel.refresh(onErrorRefreshEntries)
            })
        }
    }
}

