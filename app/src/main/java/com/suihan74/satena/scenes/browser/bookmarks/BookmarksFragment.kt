package com.suihan74.satena.scenes.browser.bookmarks

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBrowserBookmarksBinding
import com.suihan74.satena.scenes.bookmarks.BookmarksAdapter
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksTabViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserViewModel
import com.suihan74.satena.scenes.post.BookmarkPostFragment
import com.suihan74.satena.scenes.post.BookmarkPostViewModel
import com.suihan74.utilities.RecyclerViewScrollingUpdater
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.TabItem
import com.suihan74.utilities.bindings.setIconId
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarksFragment :
    Fragment(),
    ScrollableToTop,
    TabItem
{
    companion object {
        fun createInstance() = BookmarksFragment()
    }

    private val FRAGMENT_BOOKMARK_POST = "FRAGMENT_BOOKMARK_POST"

    // ------ //

    private val browserActivity : BrowserActivity
        get() = requireActivity() as BrowserActivity

    private val activityViewModel : BrowserViewModel
        get() = browserActivity.viewModel

    private val bookmarkPostViewModel : BookmarkPostViewModel
        get() = browserActivity.bookmarkPostViewModel

    /** ブクマリスト表示用のVM */
    private val bookmarksTabViewModel by lazyProvideViewModel {
        BookmarksTabViewModel(viewModel.repository, viewModel.recentBookmarks)
    }

    private val viewModel by lazyProvideViewModel {
        BookmarksViewModel(activityViewModel.bookmarksRepo)
    }

    // ------ //

    private var binding : FragmentBrowserBookmarksBinding? = null

    // ------ //

    private var onBackPressedCallback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBrowserBookmarksBinding.inflate(
            inflater,
            container,
            false
        ).apply {
            vm = viewModel
            bookmarksVM = bookmarksTabViewModel
            lifecycleOwner = viewLifecycleOwner
        }
        this.binding = binding

        initializeRecyclerView(binding)

        // 投稿エリアの表示状態を変更する
        binding.openPostAreaButton.setOnClickListener {
            val postLayout = binding.bookmarkPostFrameLayout
            // "変更後の"表示状態
            val opened = !postLayout.isVisible

            switchPostLayout(binding, opened)
        }

        binding.bottomAppBar.setOnClickListener {
            scrollToTop()
        }

        // 戻るボタンで投稿エリアを隠す
        onBackPressedCallback = activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            false
        ) {
            switchPostLayout(binding, false)
        }

        // 投稿完了したらそのブクマをリストに追加する
        bookmarkPostViewModel.setOnPostSuccessListener { bookmarkResult ->
            viewModel.viewModelScope.launch {
                viewModel.loadRecentBookmarks(requireContext())
                viewModel.repository.updateBookmark(bookmarkResult)
            }
            lifecycleScope.launchWhenResumed {
                switchPostLayout(binding, false)
            }
        }

        // 投稿エリアを作成
        if (childFragmentManager.findFragmentById(R.id.bookmark_post_frame_layout) == null) {
            val bookmarkPostFragment = BookmarkPostFragment.createInstance()
            childFragmentManager.beginTransaction()
                .replace(
                    R.id.bookmark_post_frame_layout,
                    bookmarkPostFragment,
                    FRAGMENT_BOOKMARK_POST
                )
                .commitAllowingStateLoss()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    // ------ //

    private fun initializeRecyclerView(binding: FragmentBrowserBookmarksBinding) {
        val bookmarksAdapter = BookmarksAdapter(viewLifecycleOwner).also { adapter ->
            adapter.setOnSubmitListener {
                binding.swipeLayout.isRefreshing = false
                binding.swipeLayout.isEnabled = true
                binding.progressBar.visibility = View.GONE
                adapter.loadable.value = viewModel.repository.additionalLoadable
            }

            adapter.setOnItemLongClickedListener { bookmark ->
                lifecycleScope.launch(Dispatchers.Main) {
                    viewModel.openBookmarkMenuDialog(bookmark, childFragmentManager)
                }
            }

            adapter.setOnLinkClickedListener { url ->
                browserActivity.openUrl(url)
            }

            viewModel.setAddStarButtonBinder(
                requireActivity(),
                adapter,
                viewLifecycleOwner,
                childFragmentManager
            )
        }

        val scrollingUpdater = RecyclerViewScrollingUpdater {
            lifecycleScope.launchWhenResumed {
                bookmarksAdapter.startLoading()
                viewModel.loadRecentBookmarks(requireContext(), additionalLoading = true)
                bookmarksAdapter.stopLoading()
                loadCompleted()
            }
        }

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
                viewModel.loadRecentBookmarks(requireContext(), additionalLoading = true)
                bookmarksAdapter.stopLoading()
                scrollingUpdater.isEnabled = true
            }
        }

        binding.recyclerView.let { recyclerView ->
            recyclerView.adapter = bookmarksAdapter
            recyclerView.addOnScrollListener(scrollingUpdater)
        }

        activityViewModel.loadingBookmarksEntry.observe(viewLifecycleOwner) {
            if (it == true) {
                if (!binding.swipeLayout.isRefreshing) {
                    bookmarksAdapter.submitList(null)
                    binding.swipeLayout.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }

        // スワイプしてブクマリストを更新する
        binding.swipeLayout.let { swipeLayout ->
            val activity = requireActivity()
            swipeLayout.setProgressBackgroundColorSchemeColor(activity.getThemeColor(R.attr.swipeRefreshBackground))
            swipeLayout.setColorSchemeColors(activity.getThemeColor(R.attr.colorPrimary))
            swipeLayout.setOnRefreshListener {
                lifecycleScope.launchWhenResumed {
                    viewModel.loadRecentBookmarks(requireContext())
                    swipeLayout.isRefreshing = false
                }
            }
        }
    }

    // ------ //

    /** 投稿エリアの表示状態を切り替える */
    private fun switchPostLayout(binding: FragmentBrowserBookmarksBinding, opened: Boolean) {
        // 戻るボタンの割り込みを再設定する
        onBackPressedCallback?.isEnabled = opened

        binding.openPostAreaButton.setIconId(
            if (opened) R.drawable.ic_baseline_close
            else R.drawable.ic_add_comment
        )

        TooltipCompat.setTooltipText(
            binding.openPostAreaButton,
            if (opened) context?.getString(R.string.browser_close_post_bookmark_frame)
            else context?.getString(R.string.browser_open_post_bookmark_frame)
        )

        TransitionManager.beginDelayedTransition(
            binding.bookmarkPostFrameLayout,
            Slide(Gravity.BOTTOM).also {
                it.duration = 200
            }
        )
        binding.bookmarkPostFrameLayout.setVisibility(opened)
    }

    // ------ //

    override fun scrollToTop() {
        binding?.recyclerView?.scrollToPosition(0)
    }

    // ------ //

    override fun onTabSelected() {}

    override fun onTabUnselected() {
        if (onBackPressedCallback?.isEnabled == true) {
            onBackPressedCallback?.handleOnBackPressed()
        }
    }

    override fun onTabReselected() {}
}
