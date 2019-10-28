package com.suihan74.satena.scenes.bookmarks.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks.BookmarksFragment
import com.suihan74.utilities.DividerItemDecorator

class MentionedBookmarksTabFragment : Fragment() {
    private lateinit var mRoot : View
    private var mBookmarksFragment : BookmarksFragment? = null
    private var mTargetBookmark : Bookmark? = null
    private lateinit var mTabMode : StarsTabAdapter.Tab

    companion object {
        fun createInstance(bookmarksFragment: BookmarksFragment, targetBookmark: Bookmark, tabMode: StarsTabAdapter.Tab) = MentionedBookmarksTabFragment().apply {
            mBookmarksFragment = bookmarksFragment
            mTargetBookmark = targetBookmark
            mTabMode = tabMode
        }

        private const val BUNDLE_TARGET_USER = "mTargetBookmark"
        private const val BUNDLE_TAB_MODE = "mTabMode"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putString(BUNDLE_TARGET_USER, mTargetBookmark?.user ?: "")
            putInt(BUNDLE_TAB_MODE, mTabMode.int)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_stars_tab, container, false)
        mRoot = root

        savedInstanceState?.run {
            val user = getString(BUNDLE_TARGET_USER)
            mTargetBookmark = mBookmarksFragment?.bookmarksEntry?.bookmarks?.firstOrNull { it.user == user }
            mTabMode = StarsTabAdapter.Tab.fromInt(getInt(BUNDLE_TAB_MODE))
        }

        root.findViewById<RecyclerView>(R.id.stars_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!, R.drawable.recycler_view_item_divider)!!)
            val bookmarks = mBookmarksFragment?.bookmarksEntry?.bookmarks ?: emptyList()
            val starsMap = mBookmarksFragment?.starsMap ?: emptyMap()

            val displayBookmarks = StarsTabAdapter.getMentions(mTargetBookmark!!, bookmarks, mTabMode)

            addItemDecoration(dividerItemDecoration)
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = object : MentionedBookmarksAdapter(displayBookmarks, starsMap) {
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

        //retainInstance = true
        return root
    }

    fun scrollToTop() {
        mRoot.findViewById<RecyclerView>(R.id.stars_list).scrollToPosition(0)
    }
}
