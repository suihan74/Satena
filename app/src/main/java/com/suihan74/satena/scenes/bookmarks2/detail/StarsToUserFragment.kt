package com.suihan74.satena.scenes.bookmarks2.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks2.dialog.BookmarkMenuDialog
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.fragment_stars_tab.view.*

class StarsToUserFragment : Fragment(), ScrollableToTop {
    private val detailViewModel: BookmarkDetailViewModel by lazy {
        ViewModelProviders.of(parentFragment as BookmarkDetailFragment)[BookmarkDetailViewModel::class.java]
    }

    companion object {
        fun createInstance() = StarsToUserFragment()
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
                (activity as? BookmarksActivity)?.showBookmarkDetail(item.bookmark)
            }

            override fun onItemLongClicked(item: StarWithBookmark): Boolean {
                val dialog = BookmarkMenuDialog.createInstance(item.bookmark)
                dialog.show(childFragmentManager, "bookmark_dialog")
                return true
            }
        }

        view.stars_list.apply {
            addItemDecoration(
                DividerItemDecorator(
                    ContextCompat.getDrawable(requireContext(), R.drawable.recycler_view_item_divider)!!
                )
            )
            layoutManager = LinearLayoutManager(requireContext())
            adapter = starsAdapter
        }

        // 引っ張って更新
        view.stars_swipe_layout.apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(context.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(context.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                detailViewModel.updateStarsToUser().invokeOnCompletion { e ->
                    if (e != null) {
                        context.showToast(R.string.msg_update_stars_failed)
                        Log.d("FailedToUpdateStars", Log.getStackTraceString(e))
                    }
                    this@swipeLayout.isRefreshing = false
                }
            }
        }

        // ユーザーに付けられたスターリストの更新を監視する
        detailViewModel.starsToUser.observe(this, Observer {
            starsAdapter.setStars(
                detailViewModel.getStarsWithBookmarkTo(detailViewModel.bookmark.user)
            )
        })

        return view
    }

    override fun scrollToTop() =
        view?.stars_list?.scrollToPosition(0)
}
