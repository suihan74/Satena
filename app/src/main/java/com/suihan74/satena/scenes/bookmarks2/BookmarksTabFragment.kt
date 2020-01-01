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
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.TappedActionLauncher
import com.suihan74.satena.dialogs.EntryMenuDialog
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks2.dialog.BookmarkMenuDialog
import com.suihan74.satena.scenes.bookmarks2.tab.*
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.fragment_bookmarks_tab.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarksTabFragment :
    Fragment(),
    EntryMenuDialog.Listener
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

        // adapter
        val bookmarksAdapter = object : BookmarksAdapter() {
            override fun onItemClicked(bookmark: Bookmark) {
                (activity as? BookmarksActivity)?.showBookmarkDetail(bookmark)
            }

            override fun onItemLongClicked(bookmark: Bookmark): Boolean {
                val dialog = BookmarkMenuDialog.createInstance(bookmark)
                dialog.show(childFragmentManager, "bookmark_dialog")
                return true
            }

            override fun onLinkClicked(url: String) {
                val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
                val act = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION))
                TappedActionLauncher.launch(requireContext(), act, url, childFragmentManager)
            }

            override fun onLinkLongClicked(url: String) {
                val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
                val act = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION))
                TappedActionLauncher.launch(requireContext(), act, url, childFragmentManager)
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
            bookmarksAdapter.setBookmarks(it, bookmarksEntry, userTags, ignoredUsers)
        })

        // ユーザータグの更新を監視
        activityViewModel.taggedUsers.observe(this, Observer {
            val bookmarks = viewModel.bookmarks.value
            if (bookmarks != null) {
                val bookmarksEntry = activityViewModel.bookmarksEntry.value ?: return@Observer
                val ignoredUsers = activityViewModel.ignoredUsers.value
                bookmarksAdapter.setBookmarks(bookmarks, bookmarksEntry, it, ignoredUsers)
            }
        })

        // ------ //

        return view
    }

    override fun onResume() {
        super.onResume()
        bookmarksFragmentViewModel.selectedTabViewModel.postValue(viewModel)
    }

    // --- リンクメニューダイアログの処理 --- //

    override fun onItemSelected(item: String, dialog: EntryMenuDialog) {
        val entry = dialog.entry

        when (item) {
            getString(R.string.entry_action_show_comments) ->
                TappedActionLauncher.launch(context!!, TapEntryAction.SHOW_COMMENTS, entry.url)

            getString(R.string.entry_action_show_page) ->
                TappedActionLauncher.launch(context!!, TapEntryAction.SHOW_PAGE, entry.url)

            getString(R.string.entry_action_show_page_in_browser) ->
                TappedActionLauncher.launch(context!!, TapEntryAction.SHOW_PAGE_IN_BROWSER, entry.url)
        }
    }
}
