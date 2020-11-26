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

class MentionToUserFragment : Fragment(), ScrollableToTop {
    companion object {
        fun createInstance() = MentionToUserFragment()
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
    ): View? {
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

        // 引っ張って更新（無効化）
        binding.starsSwipeLayout.isEnabled = false

        // メンション先をリストに追加
        detailViewModel.mentionsToUser.observe(viewLifecycleOwner) {
            starsAdapter.setStars(it)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun scrollToTop() {
        binding?.starsList?.scrollToPosition(0)
    }
}
