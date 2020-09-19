package com.suihan74.satena.scenes.browser.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBrowserBookmarksBinding
import com.suihan74.satena.scenes.bookmarks2.BookmarksAdapter
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.utilities.ScrollableToTop
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

        activityViewModel.bookmarksEntry.observe(viewLifecycleOwner) {
            if (it == null) {
                bookmarksAdapter.submitList(null)
            }
            else {
                bookmarksAdapter.submitList(null) {
                    lifecycleScope.launch {
                        bookmarksAdapter.setBookmarks(
                            bookmarks = it.bookmarks.filter { b -> b.comment.isNotBlank() },
                            bookmarksEntry = it,
                            taggedUsers = emptyList(),
                            ignoredUsers = emptyList(),
                            displayMutedMention = false,
                            starsEntryGetter = { null }
                        )
                    }
                }
            }
        }

        // 投稿エリアを作成
        val bookmarkPostFragment = BookmarkPostFragment.createInstance()
        childFragmentManager.beginTransaction()
            .add(R.id.bookmark_post_area, bookmarkPostFragment, FRAGMENT_BOOKMARK_POST)
            .commitAllowingStateLoss()

        return binding.root
    }

    override fun scrollToTop() {
        recycler_view?.scrollToPosition(0)
    }
}
