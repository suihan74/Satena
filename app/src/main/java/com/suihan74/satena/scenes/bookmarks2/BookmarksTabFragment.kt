package com.suihan74.satena.scenes.bookmarks2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.NotFoundException
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.tab.*
import com.suihan74.utilities.*
import com.suihan74.utilities.bindings.setDivider
import kotlinx.android.synthetic.main.fragment_bookmarks_tab.view.*

class BookmarksTabFragment :
    Fragment()
{
    /** BookmarksActivityのViewModel */
    private val activityViewModel: BookmarksViewModel by lazy {
        bookmarksActivity.viewModel
    }

    lateinit var viewModel: BookmarksTabViewModel
        private set

    private val bookmarksFragmentViewModel: BookmarksFragmentViewModel by lazy {
        ViewModelProvider(bookmarksFragment)[BookmarksFragmentViewModel::class.java]
    }

    /** このフラグメントが配置されているBookmarksActivity */
    private val bookmarksActivity
        get() = requireActivity() as BookmarksActivity

    private val bookmarksFragment
        get() = requireParentFragment() as BookmarksFragment

    companion object {
        fun createInstance(tabType: BookmarksTabType) = BookmarksTabFragment().withArguments {
            putEnum(ARG_TAB_TYPE, tabType)
        }
        private const val ARG_TAB_TYPE = "ARG_TAB_TYPE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(requireContext())
        val factory = BookmarksTabViewModel.Factory(activityViewModel, prefs)
        viewModel =
            when (requireArguments().getEnum(ARG_TAB_TYPE, BookmarksTabType.POPULAR)) {
                BookmarksTabType.POPULAR ->
                    ViewModelProvider(this, factory)[PopularTabViewModel::class.java]

                BookmarksTabType.RECENT ->
                    ViewModelProvider(this, factory)[RecentTabViewModel::class.java]

                BookmarksTabType.ALL ->
                    ViewModelProvider(this, factory)[AllBookmarksTabViewModel::class.java]

                BookmarksTabType.CUSTOM ->
                    ViewModelProvider(this, factory)[CustomTabViewModel::class.java]
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
                get() = false
        }

        // recycler view
        view.bookmarks_list.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            adapter = bookmarksAdapter

            // スクロールで追加分を取得
            addOnScrollListener(
                RecyclerViewScrollingUpdater {
                    bookmarksAdapter.startLoading()
                    viewModel.loadNextBookmarks().invokeOnCompletion { e ->
                        warnLoading(e)
                        bookmarksAdapter.stopLoading()
                        loadCompleted()
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
                    warnLoading(e)
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
        viewModel.bookmarks.observe(viewLifecycleOwner) {
            val bookmarksEntry = activityViewModel.bookmarksEntry.value ?: return@observe
            val userTags = activityViewModel.taggedUsers.value ?: emptyList()
            val ignoredUsers = activityViewModel.ignoredUsers.value
            val displayMutedMention = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING)
            bookmarksAdapter.setBookmarks(it, bookmarksEntry, userTags, ignoredUsers, displayMutedMention)
        }

        // ユーザータグの更新を監視
        activityViewModel.taggedUsers.observe(viewLifecycleOwner) {
            val bookmarks = viewModel.bookmarks.value
            if (bookmarks != null) {
                val bookmarksEntry = activityViewModel.bookmarksEntry.value ?: return@observe
                val ignoredUsers = activityViewModel.ignoredUsers.value
                val displayMutedMention = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING)
                bookmarksAdapter.setBookmarks(bookmarks, bookmarksEntry, it, ignoredUsers, displayMutedMention)
            }
        }

        // ------ //

        return view
    }

    override fun onResume() {
        super.onResume()
        bookmarksFragmentViewModel.selectedTabViewModel.value = viewModel
    }

    /** ロード失敗時のエラーメッセージ表示 */
    private fun warnLoading(e: Throwable?) {
        when (e) {
            null -> {}

            is NotFoundException -> {
                context?.showToast(R.string.msg_no_bookmarks)
                Log.w("FailedToUpdateBookmarks", Log.getStackTraceString(e))
            }

            else -> {
                context?.showToast(R.string.msg_update_bookmarks_failed)
                Log.d("FailedToUpdateBookmarks", Log.getStackTraceString(e))
            }
        }
    }
}
