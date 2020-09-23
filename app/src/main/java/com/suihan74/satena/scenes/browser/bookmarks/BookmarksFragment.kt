package com.suihan74.satena.scenes.browser.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBrowserBookmarksBinding
import com.suihan74.satena.scenes.bookmarks2.BookmarksAdapter
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.bindings.setIconId
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.getThemeColor
import kotlinx.android.synthetic.main.fragment_browser_bookmarks.*
import kotlinx.coroutines.launch

class BookmarksFragment : Fragment(), ScrollableToTop {
    companion object {
        fun createInstance() = BookmarksFragment()
    }

    private val FRAGMENT_BOOKMARK_POST = "FRAGMENT_BOOKMARK_POST"

    private val browserActivity
        get() = requireActivity() as BrowserActivity

    private val activityViewModel
        get() = browserActivity.viewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentBrowserBookmarksBinding>(
            inflater,
            R.layout.fragment_browser_bookmarks,
            container,
            false
        ).apply {
            browserViewModel = activityViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        val bookmarksAdapter = BookmarksAdapter()
        binding.recyclerView.adapter = bookmarksAdapter

        activityViewModel.loadingBookmarksEntry.observe(viewLifecycleOwner) {
            if (it == true) {
                if (!binding.swipeLayout.isRefreshing) {
                    binding.swipeLayout.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }

        activityViewModel.bookmarksEntry.observe(viewLifecycleOwner) {
            if (it == null) {
                bookmarksAdapter.submitList(null)
            }
            else {
                lifecycleScope.launch {
                    bookmarksAdapter.setBookmarks(
                        lifecycleScope,
                        bookmarks = it.bookmarks.filter { b -> b.comment.isNotBlank() },
                        bookmarksEntry = it,
                        taggedUsers = emptyList(),
                        ignoredUsers = emptyList(),
                        displayMutedMention = false,
                        starsEntryGetter = { null }
                    ) {
                        binding.swipeLayout.isRefreshing = false
                        binding.swipeLayout.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        // スワイプしてブクマリストを更新する
        binding.swipeLayout.let { swipeLayout ->
            val activity = requireActivity()
            swipeLayout.setProgressBackgroundColorSchemeColor(activity.getThemeColor(R.attr.swipeRefreshBackground))
            swipeLayout.setColorSchemeColors(activity.getThemeColor(R.attr.colorPrimary))
            swipeLayout.setOnRefreshListener {
                activityViewModel.loadBookmarksEntry(activityViewModel.url.value!!)
            }
        }

        // 投稿エリアの表示状態を変更する
        binding.openPostAreaButton.setOnClickListener {
            val postLayout = binding.bookmarkPostFrameLayout
            val opened = postLayout.isVisible

            binding.openPostAreaButton.setIconId(
                if (opened) R.drawable.ic_add_comment
                else R.drawable.ic_baseline_close
            )

            postLayout.setVisibility(!opened)
        }

        // 投稿エリアを作成
        val bookmarkPostFragment = BookmarkPostFragment.createInstance()
        childFragmentManager.beginTransaction()
            .add(R.id.bookmark_post_frame_layout, bookmarkPostFragment, FRAGMENT_BOOKMARK_POST)
            .commitAllowingStateLoss()

        return binding.root
    }

    override fun scrollToTop() {
        recycler_view?.scrollToPosition(0)
    }
}
