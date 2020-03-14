package com.suihan74.satena.scenes.bookmarks2.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.bindings.setDivider
import kotlinx.android.synthetic.main.fragment_stars_tab.view.*

class MentionFromUserFragment : Fragment(), ScrollableToTop {
    private val detailViewModel: BookmarkDetailViewModel by lazy {
        ViewModelProvider(parentFragment as BookmarkDetailFragment)[BookmarkDetailViewModel::class.java]
    }

    companion object {
        fun createInstance() = MentionFromUserFragment()
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
                (activity as? BookmarksActivity)?.onBookmarkClicked(item.bookmark)
            }

            override fun onItemLongClicked(item: StarWithBookmark) =
                (activity as? BookmarksActivity)?.onBookmarkLongClicked(item.bookmark) ?: true
        }

        view.stars_list.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = starsAdapter
        }

        // 引っ張って更新（無効化）
        view.stars_swipe_layout.isEnabled = false

        // メンション先をリストに追加
        detailViewModel.mentionsFromUser.observe(this, Observer {
            starsAdapter.setStars(it)
        })

        return view
    }

    override fun scrollToTop() =
        view?.stars_list?.scrollToPosition(0)
}
