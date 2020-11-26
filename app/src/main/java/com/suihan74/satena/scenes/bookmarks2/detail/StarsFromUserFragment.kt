package com.suihan74.satena.scenes.bookmarks2.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentStarsTabBinding
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.bindings.setDivider
import com.suihan74.utilities.extensions.getThemeColor

class StarsFromUserFragment : Fragment(), ScrollableToTop {
    companion object {
        fun createInstance() = StarsFromUserFragment()
    }

    // ------ //

    private val bookmarksActivity: BookmarksActivity
        get() = requireActivity() as BookmarksActivity

    private val activityViewModel: BookmarksViewModel
        get() = bookmarksActivity.viewModel

    private val detailViewModel: BookmarkDetailViewModel
        get() = (requireParentFragment() as BookmarkDetailFragment).viewModel

    // ------ //

    private var binding : FragmentStarsTabBinding? = null

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentStarsTabBinding.inflate(
            inflater,
            container,
            false
        )
        this.binding = binding

        val starsAdapter = object : StarsAdapter() {
            override fun onItemClicked(item: StarWithBookmark) {
                // 戻るボタンを無効化するためスターメニューを閉じる
                detailViewModel.starsMenuOpened.postValue(false)
                activityViewModel.onBookmarkClicked(bookmarksActivity, item.bookmark)
            }

            override fun onItemLongClicked(item: StarWithBookmark) : Boolean {
                activityViewModel.openBookmarkMenuDialog(bookmarksActivity, item.bookmark)
                return true
            }
        }

        binding.starsList.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = starsAdapter
        }

        // 引っ張って更新
        binding.starsSwipeLayout.apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(context.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(context.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                detailViewModel.updateStarsAll(forceUpdate = true).invokeOnCompletion {
                    this@swipeLayout.isRefreshing = false
                }
            }
        }

        // ユーザーに付けられたスターリストの更新を監視する
        detailViewModel.starsFromUser.observe(viewLifecycleOwner) {
            starsAdapter.setStars(it)
        }

        return binding.root
    }

    override fun scrollToTop() {
        binding?.starsList?.scrollToPosition(0)
    }
}
