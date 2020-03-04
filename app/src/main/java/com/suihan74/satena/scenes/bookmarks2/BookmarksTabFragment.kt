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
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.tab.*
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.fragment_bookmarks_tab.view.*

class BookmarksTabFragment :
    Fragment()
{
    /** BookmarksActivityのViewModel */
    private val activityViewModel: BookmarksViewModel by lazy {
        ViewModelProviders.of(bookmarksActivity)[BookmarksViewModel::class.java]
    }

    lateinit var viewModel: BookmarksTabViewModel
        private set

    private val bookmarksFragmentViewModel: BookmarksFragmentViewModel by lazy {
        ViewModelProviders.of(bookmarksFragment)[BookmarksFragmentViewModel::class.java]
    }

    /** このフラグメントが配置されているBookmarksActivity */
    private val bookmarksActivity
        get() = requireActivity() as BookmarksActivity

    private val bookmarksFragment
        get() = requireParentFragment() as BookmarksFragment

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

        val prefs = SafeSharedPreferences.create<PreferenceKey>(requireContext())
        val factory = BookmarksTabViewModel.Factory(activityViewModel, prefs)
        viewModel =
            when (BookmarksTabType.fromInt(requireArguments().getInt(ARG_TAB_TYPE))) {
                BookmarksTabType.POPULAR ->
                    ViewModelProviders.of(this, factory)[PopularTabViewModel::class.java]

                BookmarksTabType.RECENT ->
                    ViewModelProviders.of(this, factory)[RecentTabViewModel::class.java]

                BookmarksTabType.ALL ->
                    ViewModelProviders.of(this, factory)[AllBookmarksTabViewModel::class.java]

                BookmarksTabType.CUSTOM ->
                    ViewModelProviders.of(this, factory)[CustomTabViewModel::class.java]
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

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)

        // adapter
        val bookmarksAdapter = object : BookmarksAdapter() {
            override fun onItemClicked(bookmark: Bookmark) =
                bookmarksActivity.onBookmarkClicked(bookmark)

            override fun onItemLongClicked(bookmark: Bookmark) =
                bookmarksActivity.onBookmarkLongClicked(bookmark)

            override fun onLinkClicked(url: String) =
                bookmarksActivity.onLinkClicked(url)

            override fun onLinkLongClicked(url: String) =
                bookmarksActivity.onLinkLongClicked(url)

            override fun onEntryIdClicked(eid: Long) =
                bookmarksActivity.onEntryIdClicked(eid)

            override fun onAdditionalLoading() {
                startLoading()
                viewModel.loadNextBookmarks().invokeOnCompletion { e->
                    if (e != null) {
                        context?.showToast(R.string.msg_update_bookmarks_failed)
                        Log.d("FailedToUpdateBookmarks", Log.getStackTraceString(e))
                    }
                    stopLoading()
                }
            }

            override val nextLoadable: Boolean
                get() {
                    return false
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
                        bookmarksAdapter.startLoading()
                        viewModel.loadNextBookmarks().invokeOnCompletion { e->
                            if (e != null) {
                                context?.showToast(R.string.msg_update_bookmarks_failed)
                                Log.d("FailedToUpdateBookmarks", Log.getStackTraceString(e))
                            }
                            bookmarksAdapter.stopLoading()
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

        // スクロール処理
        viewModel.setOnScrollToTopListener {
            view?.bookmarks_list?.scrollToPosition(0)
        }
        viewModel.setOnScrollToBottomListener {
            val size = bookmarksAdapter.itemCount
            view?.bookmarks_list?.scrollToPosition(size - 1)
        }
        viewModel.setOnScrollToBookmarkListener { b ->
            val position = bookmarksAdapter.getPosition(b)
            if (position >= 0) {
                view?.bookmarks_list?.scrollToPosition(position)
            }
        }

        // --- Observers --- //

        // ブクマリストの更新を監視
        viewModel.bookmarks.observe(this, Observer {
            val bookmarksEntry = activityViewModel.bookmarksEntry.value ?: return@Observer
            val userTags = activityViewModel.taggedUsers.value ?: emptyList()
            val ignoredUsers = activityViewModel.ignoredUsers.value
            val displayMutedMention = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING)
            bookmarksAdapter.setBookmarks(it, bookmarksEntry, userTags, ignoredUsers, displayMutedMention)
        })

        // ユーザータグの更新を監視
        activityViewModel.taggedUsers.observe(this, Observer {
            val bookmarks = viewModel.bookmarks.value
            if (bookmarks != null) {
                val bookmarksEntry = activityViewModel.bookmarksEntry.value ?: return@Observer
                val ignoredUsers = activityViewModel.ignoredUsers.value
                val displayMutedMention = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING)
                bookmarksAdapter.setBookmarks(bookmarks, bookmarksEntry, it, ignoredUsers, displayMutedMention)
            }
        })

        // ------ //

        return view
    }

    override fun onResume() {
        super.onResume()
        bookmarksFragmentViewModel.selectedTabViewModel.value = viewModel
    }
}
