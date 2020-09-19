package com.suihan74.satena.scenes.bookmarks2.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.bindings.setDivider
import com.suihan74.utilities.extensions.getThemeColor
import kotlinx.android.synthetic.main.fragment_stars_tab.view.*

class StarsFromUserFragment : Fragment(), ScrollableToTop {
    private val bookmarksActivity: BookmarksActivity
        get() = requireActivity() as BookmarksActivity

    private val activityViewModel: BookmarksViewModel
        get() = bookmarksActivity.viewModel

    private val detailViewModel: BookmarkDetailViewModel
        get() = (requireParentFragment() as BookmarkDetailFragment).viewModel

    companion object {
        fun createInstance() = StarsFromUserFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stars_tab, container, false)

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

        view.stars_list.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = starsAdapter
        }

        // 引っ張って更新
        view.stars_swipe_layout.apply swipeLayout@ {
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

        return view
    }

    override fun scrollToTop() =
        view?.stars_list?.scrollToPosition(0)
}
