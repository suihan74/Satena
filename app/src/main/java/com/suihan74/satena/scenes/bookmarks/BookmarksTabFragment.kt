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
import com.suihan74.utilities.ScrollableToBottom
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getThemeColor

abstract class BookmarksTabFragment :
    Fragment(),
    ScrollableToTop,
    ScrollableToBottom
{
    /** 引っ張って更新の処理内容 */
    abstract fun reloadBookmarks()

    /** ロード完了後の処理 */
    abstract fun afterLoadedBookmarks()

    /** ブクマリスト */
    abstract val bookmarksLiveData : LiveData<List<Bookmark>>

    // ------ //

    val bookmarksActivity: BookmarksActivity
        get() = requireActivity() as BookmarksActivity

    val bookmarksViewModel: BookmarksViewModel
        get() = bookmarksActivity.bookmarksViewModel

    val contentsViewModel: ContentsViewModel
        get() = bookmarksActivity.contentsViewModel

    var binding: FragmentBookmarksTab3Binding? = null
        private set

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
    protected open fun initializeRecyclerView(binding: FragmentBookmarksTab3Binding) {
        val bookmarksAdapter = BookmarksAdapter().also { adapter ->
            adapter.setOnItemClickedListener { bookmark ->
                // 詳細画面を開く
                activity.alsoAs<BookmarkDetailOpenable> { container ->
                    contentsViewModel.openBookmarkDetail(container, bookmark)
                }
            }

            adapter.setOnItemLongClickedListener { bookmark ->
                // メニューを開く
                bookmarksViewModel.openBookmarkMenuDialog(
                    requireActivity(),
                    bookmark,
                    childFragmentManager
                )
            }

            // スターをつけるボタンの設定
            bookmarksViewModel.setAddStarButtonBinder(
                requireActivity(),
                adapter,
                viewLifecycleOwner,
                childFragmentManager,
                lifecycleScope
            )
        }
        binding.recyclerView.adapter = bookmarksAdapter

        // 引っ張って更新
        binding.swipeLayout.let { swipeLayout ->
            val context = requireContext()
            swipeLayout.setProgressBackgroundColorSchemeColor(context.getThemeColor(R.attr.swipeRefreshBackground))
            swipeLayout.setColorSchemeColors(context.getThemeColor(R.attr.colorPrimary))
            swipeLayout.setOnRefreshListener {
                reloadBookmarks()
            }
            // 初期ロード中は無効にしておく
            swipeLayout.isEnabled = false
        }

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
                    taggedUsers = repo.taggedUsers.mapNotNull { it.value.value },
                    ignoredUsers = repo.ignoredUsersCache,
                    displayMutedMention = false,
                    starsEntryGetter = { b -> repo.getStarsEntry(b)?.value }
                ) {
                    binding.swipeLayout.isRefreshing = false
                    binding.swipeLayout.isEnabled = true
                    afterLoadedBookmarks()
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
