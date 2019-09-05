package com.suihan74.satena.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.StarsEntry
import com.suihan74.satena.R
import com.suihan74.satena.adapters.MentionedBookmarksAdapter
import com.suihan74.utilities.DividerItemDecorator

class MentionedBookmarksTabFragment : Fragment() {
    private lateinit var mBookmarks : List<Bookmark>
    private lateinit var mStarsMap : Map<String, StarsEntry>

    companion object {
        fun createInstance(bookmarks: List<Bookmark>, starsMap: Map<String, StarsEntry>) = MentionedBookmarksTabFragment().apply {
            mBookmarks = bookmarks
            mStarsMap = starsMap
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_stars_tab, container, false)

        root.findViewById<RecyclerView>(R.id.stars_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!, R.drawable.recycler_view_item_divider)!!)

            addItemDecoration(dividerItemDecoration)
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = object : MentionedBookmarksAdapter(mBookmarks, mStarsMap) {
                override fun onItemClicked(user: String) {
/*
                    (activity as FragmentContainerActivity).apply {
                        showFragment(
                            BookmarkDetailFragment.createInstance(
                                target,
                                mStarsMap,
                                bookmarksEntry
                            ), null)
                    }
*/
                }
            }
        }

        return root
    }
}
