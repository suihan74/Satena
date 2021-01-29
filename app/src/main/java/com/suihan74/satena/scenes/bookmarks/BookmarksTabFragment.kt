package com.suihan74.satena.scenes.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
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
        val binding = DataBindingUtil.inflate<FragmentBookmarksTab3Binding>(
            inflater,
            R.layout.fragment_bookmarks_tab3,
            container,
            false
        ).also {
            it.bookmarks = bookmarksLiveData
            it.fragment = this
            it.lifecycleOwner = viewLifecycleOwner
        }
        this._binding = binding

        initializeRecyclerView(binding)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** ブクマリストの初期化 */
    protected open fun initializeRecyclerView(binding: FragmentBookmarksTab3Binding) {
        val bookmarksAdapter = BookmarksAdapter().also { adapter ->
            adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

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

    object BookmarksBindingAdapters {
        @JvmStatic
        @BindingAdapter("bookmarks", "fragment")
        fun bindBookmarks(
            rv: RecyclerView,
            bookmarks: List<Bookmark>?,
            fragment: BookmarksTabFragment?
        ) {
            if (bookmarks == null || fragment == null) return
            val vm = fragment.bookmarksActivity.bookmarksViewModel
            val repo = vm.repository
            rv.adapter.alsoAs<BookmarksAdapter> { adapter ->
                fragment.lifecycleScope.launch {
                    adapter.setBookmarks(
                        bookmarks = bookmarks,
                        bookmarksEntry = vm.bookmarksEntry.value,
                        taggedUsers = repo.taggedUsers.mapNotNull { it.value.value },
                        ignoredUsers = repo.ignoredUsersCache,
                        displayMutedMention = false
                    ) {
                        fragment._binding?.swipeLayout?.isRefreshing = false
                        fragment._binding?.swipeLayout?.isEnabled = true
                        fragment.afterLoadedBookmarks()
                    }
                }
            }
        }
    }
}
