package com.suihan74.satena.scenes.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBookmarksTab3Binding
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksTabViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.ContentsViewModel
import com.suihan74.utilities.ScrollableToBottom
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.launch

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

    val bookmarksActivity : BookmarksActivity
        get() = requireActivity() as BookmarksActivity

    val bookmarksViewModel : BookmarksViewModel
        get() = bookmarksActivity.bookmarksViewModel

    val contentsViewModel : ContentsViewModel
        get() = bookmarksActivity.contentsViewModel

    val viewModel by lazyProvideViewModel {
        BookmarksTabViewModel(bookmarksViewModel.repository)
    }

    private var _binding : FragmentBookmarksTab3Binding? = null
    private val binding : FragmentBookmarksTab3Binding
        get() = _binding!!

    protected val bookmarksAdapter : BookmarksAdapter
        get() = binding.recyclerView.adapter as BookmarksAdapter

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBookmarksTab3Binding.inflate(
            inflater,
            container,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        viewModel.setBookmarksLiveData(viewLifecycleOwner, bookmarksLiveData)
        initializeRecyclerView(binding)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** ブクマリストの初期化 */
    protected open fun initializeRecyclerView(binding: FragmentBookmarksTab3Binding) {
        val bookmarksAdapter = BookmarksAdapter(viewLifecycleOwner).also { adapter ->
            adapter.setOnSubmitListener {
                binding.swipeLayout.isRefreshing = false
                binding.swipeLayout.isEnabled = true
                afterLoadedBookmarks()
            }

            adapter.setOnItemClickedListener { bookmark ->
                // 詳細画面を開く
                activity.alsoAs<BookmarkDetailOpenable> { container ->
                    contentsViewModel.openBookmarkDetail(container, bookmark)
                }
            }

            adapter.setOnItemLongClickedListener { bookmark ->
                // メニューを開く
                lifecycleScope.launch {
                    bookmarksViewModel.openBookmarkMenuDialog(bookmark, childFragmentManager)
                }
            }

            adapter.setOnLinkClickedListener { url ->
                bookmarksViewModel.onLinkClicked?.invoke(url)
            }

            adapter.setOnLinkLongClickedListener { url ->
                bookmarksViewModel.onLinkLongClicked?.invoke(url)
            }

            adapter.setOnEntryIdClickedListener { id ->
                bookmarksViewModel.onEntryIdClicked?.invoke(id)
            }

            adapter.setOnEntryIdLongClickedListener { id ->
                bookmarksViewModel.onEntryIdLongClicked?.invoke(id)
            }

            // スターをつけるボタンの設定
            bookmarksViewModel.setAddStarButtonBinder(
                requireActivity(),
                adapter,
                viewLifecycleOwner,
                childFragmentManager
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
    }

    // ------ //

    override fun scrollToTop() {
        _binding?.recyclerView?.scrollToPosition(0)
    }

    override fun scrollToBottom() {
        val adapter = _binding?.recyclerView?.adapter ?: return
        _binding?.recyclerView?.scrollToPosition(adapter.itemCount - 1)
    }
}
