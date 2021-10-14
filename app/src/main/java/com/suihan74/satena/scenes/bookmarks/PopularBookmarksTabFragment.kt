package com.suihan74.satena.scenes.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.databinding.FragmentBookmarksTab3Binding
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksTabViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.ContentsViewModel
import com.suihan74.utilities.ScrollableToBottom
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.launch

/** 注目ブクマリスト表示部分のFragment */
class PopularBookmarksTabFragment :
    Fragment(),
    ScrollableToTop,
    ScrollableToBottom
{

    companion object {
        fun createInstance() = PopularBookmarksTabFragment()
    }

    // ------ //

    private val bookmarksActivity : BookmarksActivity
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

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksTab3Binding.inflate(
            inflater,
            container,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        viewModel.setBookmarksLiveData(viewLifecycleOwner, null, BookmarksTabType.POPULAR)
        initializeRecyclerView(binding)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** ブクマリストの初期化 */
    private fun initializeRecyclerView(binding: FragmentBookmarksTab3Binding) {
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

    // ------ //

    fun reloadBookmarks() {
        val context = requireActivity().applicationContext
        bookmarksViewModel.let { vm ->
            vm.viewModelScope.launch {
                vm.loadPopularBookmarks(context)
                // 取得失敗時には表示物がsubmitされないため、swipeLayoutの状態変更をここで行う
                lifecycleScope.launchWhenCreated {
                    binding.swipeLayout.isRefreshing = false
                    binding.swipeLayout.isEnabled = true
                }
            }
        }
    }

    fun afterLoadedBookmarks() {
    }
}
