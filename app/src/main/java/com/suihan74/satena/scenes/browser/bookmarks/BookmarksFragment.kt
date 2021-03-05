package com.suihan74.satena.scenes.browser.bookmarks

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.MainThread
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBrowserBookmarksBinding
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks.BookmarksAdapter
import com.suihan74.satena.scenes.bookmarks.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksTabViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserViewModel
import com.suihan74.satena.scenes.post.BookmarkPostFragment
import com.suihan74.satena.scenes.post.BookmarkPostViewModel
import com.suihan74.utilities.*
import com.suihan74.utilities.bindings.setIconId
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getThemeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ContentsViewModel(
    private val bookmarksRepo: BookmarksRepository
) : ViewModel() {
    /** ロード完了前にページ遷移した場合にロード処理を中断する */
    private var loadBookmarksEntryJob : Job? = null

    /** ローディング状態を通知する */
    val loadingBookmarksEntry = MutableLiveData<Boolean>()

    private val loadingJobMutex = Mutex()

    // ------ //

    @MainThread
    fun onCreateView(owner: LifecycleOwner, browserViewModel: BrowserViewModel) {
        browserViewModel.url.observe(owner, Observer { url ->
            viewModelScope.launch {
                loadBookmarksEntry(url)
            }
        })
    }

    // ------ //

    /** BookmarksEntryを更新 */
    private suspend fun loadBookmarksEntry(url: String) {
        loadingJobMutex.withLock {
            loadBookmarksEntryJob?.cancel()
            if (!URLUtil.isNetworkUrl(url)) return@withLock
            loadBookmarksEntryJob = viewModelScope.launch(Dispatchers.Main) {
                runCatching {
                    loadingBookmarksEntry.value = true
                    bookmarksRepo.loadBookmarks(url)
                }
                loadingJobMutex.withLock {
                    loadBookmarksEntryJob = null
                    loadingBookmarksEntry.value = false
                }
            }
        }
    }
}

// ------ //

class BookmarksFragment :
    Fragment(),
    ScrollableToTop,
    TabItem
{
    companion object {
        fun createInstance() = BookmarksFragment()
    }

    private val FRAGMENT_BOOKMARK_POST = "FRAGMENT_BOOKMARK_POST"

    // ------ //

    private val browserActivity : BrowserActivity
        get() = requireActivity() as BrowserActivity

    private val activityViewModel : BrowserViewModel
        get() = browserActivity.viewModel

    private val bookmarkPostViewModel : BookmarkPostViewModel
        get() = browserActivity.bookmarkPostViewModel

    /** ブクマリスト表示用のVM */
    private val bookmarksTabViewModel by lazyProvideViewModel {
        BookmarksTabViewModel(viewModel.repository)
    }

    private val viewModel by lazyProvideViewModel {
        BookmarksViewModel(activityViewModel.bookmarksRepo)
    }

    private val contentsViewModel by lazyProvideViewModel {
        ContentsViewModel(activityViewModel.bookmarksRepo)
    }

    // ------ //

    private var _binding : FragmentBrowserBookmarksBinding? = null
    private val binding get() = _binding!!

    // ------ //

    private var onBackPressedCallback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowserBookmarksBinding.inflate(
            inflater,
            container,
            false
        ).apply {
            vm = viewModel
            bookmarksVM = bookmarksTabViewModel
            contentsVM = contentsViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        contentsViewModel.onCreateView(viewLifecycleOwner, activityViewModel)
        initializeRecyclerView(binding)
        initializeBottomBar(binding)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ------ //

    private fun initializeBottomBar(binding: FragmentBrowserBookmarksBinding) {
        // 投稿エリアの表示状態を変更する
        binding.openPostAreaButton.setOnClickListener {
            val postLayout = binding.bookmarkPostFrameLayout
            // "変更後の"表示状態
            val opened = !postLayout.isVisible

            switchPostLayout(binding, opened)
        }

        // 戻るボタンで投稿エリアを隠す
        onBackPressedCallback = activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            false
        ) {
            switchPostLayout(binding, false)
        }

        // 投稿完了したらそのブクマをリストに追加する
        bookmarkPostViewModel.setOnPostSuccessListener { bookmarkResult ->
            viewModel.viewModelScope.launch {
                viewModel.loadRecentBookmarks(requireContext())
                viewModel.repository.updateBookmark(bookmarkResult)
            }
            lifecycleScope.launchWhenResumed {
                switchPostLayout(binding, false)
            }
        }

        // 表示するブクマリストを選択する
        binding.bookmarksTypeSpinner.also { spinner ->
            val tabs = BookmarksTabType.values()
            val labels = tabs.map { getText(it.textId) }
            spinner.adapter = object : ArrayAdapter<CharSequence>(
                requireContext(),
                R.layout.spinner_browser_bookmarks_tabs,
                labels
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    view.alsoAs<TextView> {
                        it.setPadding(0, 0, 0, 0)
                    }
                    return view
                }
            }
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    binding.recyclerView.adapter.alsoAs<BookmarksAdapter> { adapter ->
                        adapter.submitList(null) {
                            val tabType = BookmarksTabType.fromOrdinal(position)
                            bookmarksTabViewModel.setBookmarksLiveData(
                                viewLifecycleOwner,
                                viewModel.bookmarksLiveData(tabType),
                                tabType
                            )
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            // 初期表示タブ
            val prefs = SafeSharedPreferences.create<PreferenceKey>(requireContext())
            val tabPosition = prefs.getInt(PreferenceKey.BOOKMARKS_INITIAL_TAB)
            spinner.setSelection(tabPosition, false)
        }

        // カスタムブクマリストの表示項目を設定する
        binding.customBookmarksPrefButton.setOnClickListener {
            viewModel.openCustomTabSettingsDialog(childFragmentManager)
        }

        // 投稿エリアを作成
        if (childFragmentManager.findFragmentById(R.id.bookmark_post_frame_layout) == null) {
            val bookmarkPostFragment = BookmarkPostFragment.createInstance()
            childFragmentManager.beginTransaction()
                .replace(
                    R.id.bookmark_post_frame_layout,
                    bookmarkPostFragment,
                    FRAGMENT_BOOKMARK_POST
                )
                .commitAllowingStateLoss()
        }
    }

    private fun initializeRecyclerView(binding: FragmentBrowserBookmarksBinding) {
        val bookmarksAdapter = BookmarksAdapter(viewLifecycleOwner).also { adapter ->
            adapter.setOnSubmitListener {
                binding.swipeLayout.isRefreshing = false
                binding.swipeLayout.isEnabled = true
                binding.progressBar.visibility = View.GONE
                adapter.loadable.value =
                    bookmarksTabViewModel.bookmarksTabType.value != BookmarksTabType.POPULAR && viewModel.repository.additionalLoadable
            }

            adapter.setOnItemLongClickedListener { bookmark ->
                lifecycleScope.launch(Dispatchers.Main) {
                    viewModel.openBookmarkMenuDialog(bookmark, childFragmentManager)
                }
            }

            adapter.setOnLinkClickedListener { url ->
                browserActivity.openUrl(url)
            }

            viewModel.setAddStarButtonBinder(
                requireActivity(),
                adapter,
                viewLifecycleOwner,
                childFragmentManager
            )
        }

        val scrollingUpdater = RecyclerViewScrollingUpdater {
            lifecycleScope.launchWhenResumed {
                bookmarksAdapter.startLoading()
                viewModel.loadRecentBookmarks(requireContext(), additionalLoading = true)
                bookmarksAdapter.stopLoading()
                loadCompleted()
            }
        }

        bookmarksAdapter.setOnAdditionalLoadingListener {
            // "引っ張って更新"中には実行しない
            if (binding.swipeLayout.isRefreshing) {
                return@setOnAdditionalLoadingListener
            }

            // スクロールによる追加ロード中には実行しない
            if (scrollingUpdater.isLoading) {
                return@setOnAdditionalLoadingListener
            }

            bookmarksAdapter.startLoading()
            scrollingUpdater.isEnabled = false
            lifecycleScope.launch(Dispatchers.Main) {
                viewModel.loadRecentBookmarks(requireContext(), additionalLoading = true)
                bookmarksAdapter.stopLoading()
                scrollingUpdater.isEnabled = true
            }
        }

        binding.recyclerView.let { recyclerView ->
            recyclerView.adapter = bookmarksAdapter
            recyclerView.addOnScrollListener(scrollingUpdater)
        }

        contentsViewModel.loadingBookmarksEntry.observe(viewLifecycleOwner, Observer {
            if (it == true && !binding.swipeLayout.isRefreshing) {
                bookmarksAdapter.submitList(emptyList())
                binding.swipeLayout.isEnabled = false
            }
        })

        // スワイプしてブクマリストを更新する
        binding.swipeLayout.let { swipeLayout ->
            val activity = requireActivity()
            swipeLayout.setProgressBackgroundColorSchemeColor(activity.getThemeColor(R.attr.swipeRefreshBackground))
            swipeLayout.setColorSchemeColors(activity.getThemeColor(R.attr.colorPrimary))
            swipeLayout.setOnRefreshListener {
                lifecycleScope.launchWhenResumed {
                    viewModel.loadRecentBookmarks(requireContext())
                    swipeLayout.isRefreshing = false
                }
            }
        }
    }

    // ------ //

    /** 投稿エリアの表示状態を切り替える */
    private fun switchPostLayout(binding: FragmentBrowserBookmarksBinding, opened: Boolean) {
        // 戻るボタンの割り込みを再設定する
        onBackPressedCallback?.isEnabled = opened

        binding.openPostAreaButton.setIconId(
            if (opened) R.drawable.ic_baseline_close
            else R.drawable.ic_add_comment
        )

        TooltipCompat.setTooltipText(
            binding.openPostAreaButton,
            if (opened) context?.getString(R.string.browser_close_post_bookmark_frame)
            else context?.getString(R.string.browser_open_post_bookmark_frame)
        )

        TransitionManager.beginDelayedTransition(
            binding.bookmarkPostFrameLayout,
            Slide(Gravity.BOTTOM).also {
                it.duration = 200
            }
        )
        binding.bookmarkPostFrameLayout.setVisibility(opened)
    }

    // ------ //

    override fun scrollToTop() {
        _binding?.recyclerView?.scrollToPosition(0)
    }

    // ------ //

    override fun onTabSelected() {}

    override fun onTabUnselected() {
        if (onBackPressedCallback?.isEnabled == true) {
            onBackPressedCallback?.handleOnBackPressed()
        }
    }

    override fun onTabReselected() {}
}
