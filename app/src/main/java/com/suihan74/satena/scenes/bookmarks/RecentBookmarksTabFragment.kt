package com.suihan74.satena.scenes.bookmarks

import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.databinding.FragmentBookmarksTab3Binding
import com.suihan74.satena.scenes.bookmarks2.BookmarksAdapter
import com.suihan74.utilities.RecyclerViewScrollingUpdater
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getEnum
import com.suihan74.utilities.extensions.putEnum
import com.suihan74.utilities.extensions.withArguments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 新着ブクマリスト(に依存するブクマリスト)表示部分のFragment */
class RecentBookmarksTabFragment : BookmarksTabFragment() {

    companion object {
        fun createInstance(tabType: BookmarksTabType) = RecentBookmarksTabFragment().withArguments {
            putEnum(ARG_TAB_TYPE, tabType) { it.ordinal }
        }

        private const val ARG_TAB_TYPE = "ARG_TAB_TYPE"
    }

    // ------ //

    override  val bookmarksLiveData : LiveData<List<Bookmark>>
        get() = when(requireArguments().getEnum<BookmarksTabType>(ARG_TAB_TYPE)) {
            BookmarksTabType.RECENT ->
                bookmarksViewModel.recentBookmarks

            BookmarksTabType.ALL ->
                bookmarksViewModel.allBookmarks

            BookmarksTabType.CUSTOM ->
                bookmarksViewModel.customBookmarks

            else -> throw IllegalArgumentException()
        }

    override fun reloadBookmarks() {
        bookmarksViewModel.reloadBookmarks()
    }

    override fun afterLoadedBookmarks() {
        scrollingUpdater?.isEnabled = true
        binding?.recyclerView?.adapter.alsoAs<BookmarksAdapter> { adapter ->
            adapter.additionalLoadable = bookmarksViewModel.repository.additionalLoadable
        }
    }

    // ------ //

    private var scrollingUpdater : RecyclerViewScrollingUpdater? = null

    // ------ //

    /** ブクマリストの初期化 */
    override fun initializeRecyclerView(binding: FragmentBookmarksTab3Binding) {
        super.initializeRecyclerView(binding)

        val bookmarksAdapter = binding.recyclerView.adapter as? BookmarksAdapter ?: return

        // 下端までスクロールで追加分取得
        val scrollingUpdater = RecyclerViewScrollingUpdater {
            // "引っ張って更新"中には実行しない
            if (binding.swipeLayout.isRefreshing) {
                loadCompleted()
                return@RecyclerViewScrollingUpdater
            }

            bookmarksAdapter.startLoading()
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    bookmarksViewModel.repository.loadRecentBookmarks(additionalLoading = true)
                }
                finally {
                    bookmarksAdapter.stopLoading()
                    loadCompleted()
                }
            }
        }
        this.scrollingUpdater = scrollingUpdater
        // 少なくとも一度以上リストが更新されてから追加ロードを有効にする
        scrollingUpdater.isEnabled = false
        binding.recyclerView.addOnScrollListener(scrollingUpdater)

        // 「続きを読み込む」ボタン押下による明示的な追加ロード
        bookmarksAdapter.setOnAdditionalLoadingListener {
            // "引っ張って更新"中には実行しない
            if (binding.swipeLayout.isRefreshing) {
                return@setOnAdditionalLoadingListener
            }

            // スクロールによる追加ロード中には実行しない
            if (scrollingUpdater.isLoading) {
                return@setOnAdditionalLoadingListener
            }

            bookmarksAdapter.startLoading()
            scrollingUpdater.isEnabled = false
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    bookmarksViewModel.repository.loadRecentBookmarks(additionalLoading = true)
                }
                finally {
                    bookmarksAdapter.stopLoading()
                    scrollingUpdater.isEnabled = true
                }
            }
        }
    }

}
