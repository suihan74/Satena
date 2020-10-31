package com.suihan74.satena.scenes.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBookmarksTab3Binding
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.ContentsViewModel
import com.suihan74.satena.scenes.bookmarks2.BookmarksAdapter
import com.suihan74.utilities.RecyclerViewScrollingUpdater
import com.suihan74.utilities.ScrollableToBottom
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** ブクマリスト表示部分のFragment */
class BookmarksTabFragment :
    Fragment(),
    ScrollableToTop,
    ScrollableToBottom
{

    companion object {
        fun createInstance(tabType: BookmarksTabType) = BookmarksTabFragment().withArguments {
            putEnum(ARG_TAB_TYPE, tabType) { it.ordinal }
        }

        private const val ARG_TAB_TYPE = "ARG_TAB_TYPE"
    }

    // ------ //

    val bookmarksActivity : BookmarksActivity
        get() = requireActivity() as BookmarksActivity

    val bookmarksViewModel : BookmarksViewModel
        get() = bookmarksActivity.bookmarksViewModel

    val contentsViewModel : ContentsViewModel
        get() = bookmarksActivity.contentsViewModel

    val bookmarksLiveData : LiveData<List<Bookmark>>
        get() = when(requireArguments().getEnum<BookmarksTabType>(ARG_TAB_TYPE)) {
            BookmarksTabType.POPULAR ->
                bookmarksViewModel.popularBookmarks

            BookmarksTabType.RECENT ->
                bookmarksViewModel.recentBookmarks

            BookmarksTabType.ALL ->
                bookmarksViewModel.allBookmarks

            BookmarksTabType.CUSTOM ->
                bookmarksViewModel.customBookmarks

            else -> throw IllegalArgumentException()
        }

    private var binding : FragmentBookmarksTab3Binding? = null

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentBookmarksTab3Binding>(
            inflater,
            R.layout.fragment_bookmarks_tab3,
            container,
            false
        ).also {
            it.lifecycleOwner = viewLifecycleOwner
        }
        this.binding = binding

        initializeRecyclerView(binding)

        return binding.root
    }

    /** ブクマリストの初期化 */
    private fun initializeRecyclerView(binding: FragmentBookmarksTab3Binding) {
        val bookmarksAdapter = BookmarksAdapter().also { adapter ->
            adapter.setOnItemClickedListener { bookmark ->
                // 詳細画面を開く
                activity.alsoAs<BookmarkDetailOpenable> { container ->
                    contentsViewModel.openBookmarkDetail(container, bookmark)
                }
            }

            adapter.setOnItemLongClickedListener { bookmark ->
                // メニューを開く
                bookmarksViewModel.openBookmarkMenuDialog(requireActivity(), bookmark, childFragmentManager)
            }
        }
        binding.recyclerView.adapter = bookmarksAdapter

        // 引っ張って更新
        binding.swipeLayout.let { swipeLayout ->
            val context = requireContext()
            swipeLayout.setProgressBackgroundColorSchemeColor(context.getThemeColor(R.attr.swipeRefreshBackground))
            swipeLayout.setColorSchemeColors(context.getThemeColor(R.attr.colorPrimary))
            swipeLayout.setOnRefreshListener {
                bookmarksViewModel.reloadBookmarks()
            }
            // 初期ロード中は無効にしておく
            swipeLayout.isEnabled = false
        }

        // 下端までスクロールで追加分取得
        val scrollingUpdater = RecyclerViewScrollingUpdater {
            // "引っ張って更新"中には実行しない
            if (binding.swipeLayout.isRefreshing) {
                loadCompleted()
                return@RecyclerViewScrollingUpdater
            }

            bookmarksAdapter.startLoading()
            lifecycleScope.launch(Dispatchers.Main) {
                bookmarksViewModel.repository.loadRecentBookmarks(additionalLoading = true)
                bookmarksAdapter.stopLoading()
                loadCompleted()
            }
        }
        // 少なくとも一度以上リストが更新されてから追加ロードを有効にする
        scrollingUpdater.isEnabled = false
        binding.recyclerView.addOnScrollListener(scrollingUpdater)

        // 取得したリストを表示
        bookmarksLiveData.observe(viewLifecycleOwner) {
            if (it == null) {
                bookmarksAdapter.submitList(null)
            }
            else {
                val repo = bookmarksViewModel.repository
                bookmarksAdapter.setBookmarks(
                    lifecycleScope,
                    bookmarks = it,
                    bookmarksEntry = bookmarksViewModel.bookmarksEntry.value,
                    taggedUsers = repo.taggedUsers,
                    ignoredUsers = repo.ignoredUsersCache,
                    displayMutedMention = false,
                    starsEntryGetter = { b -> repo.getStarsEntry(b)?.value }
                ) {
                    binding.swipeLayout.isRefreshing = false
                    binding.swipeLayout.isEnabled = true
                    scrollingUpdater.isEnabled = true
                }
            }
        }
    }

    // ------ //

    override fun scrollToTop() {
        binding?.recyclerView?.scrollToPosition(0)
    }

    override fun scrollToBottom() {
        val adapter = binding?.recyclerView?.adapter ?: return
        binding?.recyclerView?.scrollToPosition(adapter.itemCount - 1)
    }
}
