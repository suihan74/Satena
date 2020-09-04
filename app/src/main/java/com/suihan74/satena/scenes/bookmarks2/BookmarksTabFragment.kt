package com.suihan74.satena.scenes.bookmarks2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.NotFoundException
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.tab.BookmarksTabViewModel
import com.suihan74.utilities.*
import com.suihan74.utilities.bindings.setDivider
import kotlinx.android.synthetic.main.fragment_bookmarks_tab.view.*
import kotlinx.coroutines.launch

class BookmarksTabFragment :
    Fragment()
{
    /** BookmarksActivityのViewModel */
    private val activityViewModel: BookmarksViewModel by lazy {
        bookmarksActivity.viewModel
    }

    val viewModel: BookmarksTabViewModel by lazy {
        val tabType = requireArguments().getEnum(ARG_TAB_TYPE, BookmarksTabType.POPULAR)
        val prefs = SafeSharedPreferences.create<PreferenceKey>(requireContext())
        val factory = BookmarksTabViewModel.Factory(tabType, activityViewModel, prefs)
        ViewModelProvider(this, factory)[factory.key, BookmarksTabViewModel::class.java].apply {
            init()
        }
    }

    private val bookmarksFragmentViewModel: BookmarksFragmentViewModel
        get() = bookmarksFragment.viewModel

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks_tab, container, false)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)

        // adapter
        val bookmarksAdapter = object : BookmarksAdapter(viewLifecycleOwner, viewModel) {
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
                viewModel.loadNextBookmarks(
                    onSuccess = {},
                    onError = { e -> warnLoading(e) },
                    onFinally = { stopLoading(viewModel.additionalLoadable) }
                )
            }
        }
        bookmarksAdapter.additionalLoadable = viewModel.additionalLoadable

        // recycler view
        view.bookmarks_list.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            adapter = bookmarksAdapter

            // スクロールで追加分を取得
            addOnScrollListener(
                RecyclerViewScrollingUpdater {
                    bookmarksAdapter.startLoading()
                    viewModel.loadNextBookmarks(
                        onSuccess = {},
                        onError = { warnLoading(it) },
                        onFinally = {
                            bookmarksAdapter.stopLoading(viewModel.additionalLoadable)
                            loadCompleted()
                        }
                    )
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

        // スター数の変化を監視する
        activityViewModel.repository.allStarsLiveData.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                bookmarksAdapter.updateStar(it)
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
