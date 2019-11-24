package com.suihan74.satena.scenes.bookmarks.detail

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.scenes.bookmarks.BookmarksFragment
import com.suihan74.utilities.BookmarkCommentDecorator

class StarsTabAdapter(
    private val viewPager: ViewPager,
    detailFragment: BookmarkDetailFragment,
    private val bookmark : Bookmark
) : FragmentPagerAdapter(detailFragment.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    enum class Tab(val int: Int) {
        TO_USER(0),
        FROM_USER(1),
        MENTION_TO_USER(2),
        MENTION_FROM_USER(3);

        companion object {
            fun fromInt(i: Int) = values().first { it.int == i }
        }
    }

    private val tabs = ArrayList<Pair<Tab, () -> Fragment>>()

    companion object {
        fun getMentionsFromUser(bookmark: Bookmark, bookmarks: List<Bookmark>): List<Bookmark> {
            val analyzed = BookmarkCommentDecorator.convert(bookmark.comment)
            return bookmarks.filter { b -> analyzed.ids.contains(b.user) }
        }

        fun getMentionsToUser(bookmark: Bookmark, bookmarks: List<Bookmark>): List<Bookmark> {
            val targetUser = bookmark.user
            return bookmarks.filter { b -> b.comment.contains("id:$targetUser") }
        }

        fun getMentions(bookmark: Bookmark, bookmarks: List<Bookmark>, tabMode: Tab) = when (tabMode) {
            Tab.MENTION_FROM_USER -> getMentionsFromUser(
                bookmark,
                bookmarks
            )
            Tab.MENTION_TO_USER -> getMentionsToUser(
                bookmark,
                bookmarks
            )
            else -> emptyList()
        }
    }

    fun updateTabs(bookmarksFragment: BookmarksFragment) {
        val analyzed = BookmarkCommentDecorator.convert(bookmark.comment)
        val ids = analyzed.ids
        val targetUser = bookmark.user

        val bookmarksEntry = bookmarksFragment.bookmarksEntry!!
        val containsMentionsFromUser = bookmarksEntry.bookmarks.any { b -> ids.contains(b.user) }
        val containsMentionsToUser = bookmarksEntry.bookmarks.any { b -> b.comment.contains("id:$targetUser") }

        tabs.run {
            clear()
            add(Tab.TO_USER to {
                StarsTabFragment.createInstance(bookmark, StarsAdapter.StarsTabMode.TO_USER)
            })
            add(Tab.FROM_USER to {
                StarsTabFragment.createInstance(bookmark, StarsAdapter.StarsTabMode.FROM_USER)
            })
            if (containsMentionsToUser) {
                add(Tab.MENTION_TO_USER to {
                    MentionedBookmarksTabFragment.createInstance(bookmark, Tab.MENTION_TO_USER)
                })
            }
            if (containsMentionsFromUser) {
                add(Tab.MENTION_FROM_USER to {
                    MentionedBookmarksTabFragment.createInstance(bookmark, Tab.MENTION_FROM_USER)
                })
            }
        }

        notifyDataSetChanged()
    }

    fun clearTabs() {
        tabs.clear()
        notifyDataSetChanged()
    }

    override fun getItem(position: Int) = tabs[position].second()

    override fun getPageTitle(position: Int) = when (Tab.fromInt(position)) {
        Tab.TO_USER -> "★ → ${bookmark.user}"
        Tab.FROM_USER -> "${bookmark.user} → ★"
        Tab.MENTION_TO_USER -> "mention → ${bookmark.user}"
        Tab.MENTION_FROM_USER -> "${bookmark.user} → mention"
    }

    override fun getCount() = tabs.size

    fun findFragment(position: Int) =
        instantiateItem(viewPager, position) as Fragment
}
