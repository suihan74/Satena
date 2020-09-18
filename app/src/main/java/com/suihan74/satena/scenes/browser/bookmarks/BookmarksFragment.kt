package com.suihan74.satena.scenes.browser.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.suihan74.satena.R

class BookmarksFragment : Fragment() {
    companion object {
        fun createInstance() = BookmarksFragment()
    }

    private val FRAGMENT_BOOKMARK_POST = "FRAGMENT_BOOKMARK_POST"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_browser_bookmarks, container, false)

        // 投稿エリアを作成
        val bookmarkPostFragment = BookmarkPostFragment.createInstance()
        childFragmentManager.beginTransaction()
            .add(R.id.bookmark_post_area, bookmarkPostFragment, FRAGMENT_BOOKMARK_POST)
            .commitAllowingStateLoss()

        return root
    }
}
