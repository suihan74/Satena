package com.suihan74.satena.adapters.tabs

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.adapters.StarsAdapter
import com.suihan74.satena.fragments.BookmarkDetailFragment
import com.suihan74.satena.fragments.BookmarksFragment
import com.suihan74.satena.fragments.MentionedBookmarksTabFragment
import com.suihan74.satena.fragments.StarsTabFragment
import com.suihan74.utilities.BookmarkCommentDecorator

class StarsTabAdapter(
    bookmarksFragment : BookmarksFragment,
    detailFragment: BookmarkDetailFragment,
    private val bookmark : Bookmark
) : FragmentPagerAdapter(detailFragment.childFragmentManager) {

    enum class Tab(val int: Int) {
        TO_USER(0),
        FROM_USER(1),
        MENTION_TO_USER(2),
        MENTION_FROM_USER(3);

        companion object {
            fun fromInt(i: Int) = values().first { it.int == i }
        }
    }

    private val tabs = ArrayList<Pair<Tab, ()->Fragment>>()
    private val starsMap = bookmarksFragment.starsMap

    init {
        val analyzed = BookmarkCommentDecorator.convert(bookmark.comment)
        val ids = analyzed.ids
        val targetUser = bookmark.user

        val bookmarksEntry = bookmarksFragment.bookmarksEntry!!
        val mentionsFromUser = bookmarksEntry.bookmarks.filter { b -> ids.contains(b.user) }
        val mentionsToUser = bookmarksEntry.bookmarks.filter { b -> b.comment.contains("id:$targetUser") }

        tabs.apply {
            clear()
            add(Tab.TO_USER to {
                StarsTabFragment.createInstance(bookmark, StarsAdapter.StarsTabMode.TO_USER)
            })
            add(Tab.FROM_USER to {
                StarsTabFragment.createInstance(bookmark, StarsAdapter.StarsTabMode.FROM_USER)
            })
            if (mentionsToUser.isNotEmpty()) {
                add(Tab.MENTION_TO_USER to {
                    MentionedBookmarksTabFragment.createInstance(mentionsToUser, starsMap)
                })
            }
            if (mentionsFromUser.isNotEmpty()) {
                add(Tab.MENTION_FROM_USER to {
                    MentionedBookmarksTabFragment.createInstance(mentionsFromUser, starsMap)
                })
            }
        }
    }

    override fun getItem(position: Int) = tabs[position].second()

    override fun getPageTitle(position: Int) = when (tabs[position].first) {
        Tab.TO_USER -> "★ → ${bookmark.user}"
        Tab.FROM_USER -> "${bookmark.user} → ★"
        Tab.MENTION_TO_USER -> "mention → ${bookmark.user}"
        Tab.MENTION_FROM_USER -> "${bookmark.user} → mention"
    }

    override fun getCount() = tabs.size
}
