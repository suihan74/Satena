package com.suihan74.satena.scenes.bookmarks2

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
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks2.tab.AllBookmarksTabViewModel
import com.suihan74.satena.scenes.bookmarks2.tab.BookmarksTabViewModel
import com.suihan74.satena.scenes.bookmarks2.tab.PopularTabViewModel
import com.suihan74.satena.scenes.bookmarks2.tab.RecentTabViewModel
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.fragment_bookmarks_tab.view.*

class BookmarksTabFragment : Fragment(), ScrollableToTop {
    /** BookmarksActivityのViewModel */
    private val activityViewModel: BookmarksViewModel by lazy {
        ViewModelProviders.of(bookmarksActivity)[BookmarksViewModel::class.java]
    }

    private lateinit var viewModel: BookmarksTabViewModel

    /** このフラグメントが配置されているBookmarksActivity */
    val bookmarksActivity
        get() = requireActivity() as BookmarksActivity

    companion object {
        fun createInstance(tabType: BookmarksTabType) = BookmarksTabFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TAB_TYPE, tabType.ordinal)
            }
        }
        private const val ARG_TAB_TYPE = "ARG_TAB_TYPE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = BookmarksTabViewModel.Factory(activityViewModel)
        viewModel =
            when (BookmarksTabType.fromInt(arguments!!.getInt(ARG_TAB_TYPE))) {
                BookmarksTabType.POPULAR ->
                    ViewModelProviders.of(this, factory)[PopularTabViewModel::class.java]

                BookmarksTabType.RECENT ->
                    ViewModelProviders.of(this, factory)[RecentTabViewModel::class.java]

                BookmarksTabType.ALL ->
                    ViewModelProviders.of(this, factory)[AllBookmarksTabViewModel::class.java]

                else ->
                    ViewModelProviders.of(this, factory)[PopularTabViewModel::class.java]
            }

        if (savedInstanceState == null) {
            viewModel.init()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks_tab, container, false)

        // adapter
        val bookmarksAdapter = object : BookmarksAdapter() {
            override fun onItemClicked(bookmark: Bookmark) {
                (activity as? BookmarksActivity)?.showBookmarkDetail(bookmark)
            }
        }

        // recycler view
        view.bookmarks_list.apply {
            val dividerItemDecoration = DividerItemDecorator(
                ContextCompat.getDrawable(requireContext(), R.drawable.recycler_view_item_divider)!!
            )
            addItemDecoration(dividerItemDecoration)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookmarksAdapter

            // スクロールで追加分を取得
            addOnScrollListener(
                object : RecyclerViewScrollingUpdater(bookmarksAdapter) {
                    override fun load() {
                        viewModel.loadNextBookmarks().invokeOnCompletion { e->
                            if (e != null) {
                                context?.showToast(R.string.msg_update_bookmarks_failed)
                                Log.d("FailedToUpdateBookmarks", Log.getStackTraceString(e))
                            }
                            loadCompleted()
                        }
                    }
                }
            )
        }

        // 引っ張って更新
        view.bookmarks_swipe_layout.apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(context.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(context.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                viewModel.updateBookmarks().invokeOnCompletion { e ->
                    if (e != null) {
                        context.showToast(R.string.msg_update_bookmarks_failed)
                        Log.d("FailedToUpdateBookmarks", Log.getStackTraceString(e))
                    }
                    this@swipeLayout.isRefreshing = false
                }
            }
        }


        viewModel.bookmarks.observe(this, Observer {
            bookmarksAdapter.setBookmarks(it)
        })

        // ------ //

        return view
    }

    /** リストを一番上までスクロールする */
    override fun scrollToTop() =
        view?.bookmarks_list?.scrollToPosition(0)
}
