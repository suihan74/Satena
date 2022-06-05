package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.NetworkReceiver
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentEntriesTab2Binding
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.dialog.ExcludedEntriesDialog
import com.suihan74.satena.scenes.entries2.dialog.ExcludedEntry
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getEnum
import com.suihan74.utilities.provideViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

abstract class EntriesTabFragmentBase : Fragment(), ScrollableToTop {
    companion object {
        /** このタブを表示しているEntriesFragmentのID */
        const val ARG_FRAGMENT_VIEW_MODEL_KEY = "ARG_FRAGMENT_VIEW_MODEL_KEY"

        /** このタブで表示するエントリのカテゴリ */
        const val ARG_CATEGORY = "ARG_CATEGORY"

        /** このタブの表示位置 */
        const val ARG_TAB_POSITION = "ARG_TAB_POSITION"
    }

    protected val entriesActivity : EntriesActivity
        get() = requireActivity() as EntriesActivity

    /** EntriesActivityのViewModel */
    private val activityViewModel : EntriesViewModel
        get() = entriesActivity.viewModel

    /** タブの表示内容に関するViewModel */
    protected val viewModel by lazy {
        provideViewModel(this) { // abstract classであるためコンストラクタでthis参照させないようにする
            val arguments = requireArguments()
            val category = arguments.getEnum<Category>(ARG_CATEGORY)!!
            val tabPosition = arguments.getInt(ARG_TAB_POSITION, 0)

            EntriesTabFragmentViewModel(
                activityViewModel.repository,
                SatenaApplication.instance.readEntriesRepository,
                category,
                tabPosition
            )
        }
    }

    private var _binding : FragmentEntriesTab2Binding? = null
    private val binding get() = _binding!!

    protected val entriesList : RecyclerView
        get() = binding.entriesList

    private val updateEntryActionFlow = SatenaApplication.instance.actionsRepository.updateEntryActionFlow

    /** 親のEntriesFragmentのViewModel */
    protected val parentViewModel : EntriesFragmentViewModel?
        get() =
            requireArguments().getString(ARG_FRAGMENT_VIEW_MODEL_KEY)?.let { key ->
                ViewModelProvider(requireActivity())[key, EntriesFragmentViewModel::class.java]
            }

    /** リスト更新失敗時に呼ばれる */
    protected fun onErrorRefreshEntries(e: Throwable) {
        runCatching {
            Log.e("error", Log.getStackTraceString(e))
            activity?.showToast(R.string.msg_update_entries_failed)
        }
    }

    /**
     * コンテンツを表示するRecyclerViewを設定する
     */
    abstract fun initializeRecyclerView(entriesList: RecyclerView, swipeLayout: SwipeRefreshLayout)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEntriesTab2Binding.inflate(inflater, container, false).also {
            it.lifecycleOwner = viewLifecycleOwner
            it.vm = viewModel
        }

        binding.entriesList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                entriesActivity.updateScrollBehavior(dx, dy)
            }
        })

        // 通信状態の変更を監視
        // リスト未ロード状態なら再試行する
        SatenaApplication.instance.networkReceiver.state.observe(viewLifecycleOwner, { state ->
            if (state == NetworkReceiver.State.CONNECTED && viewModel.filteredEntries.value.isNullOrEmpty()) {
                lifecycleScope.launchWhenResumed {
                    runCatching { viewModel.reloadLists() }
                        .onFailure { onErrorRefreshEntries(it) }
                }
            }
        })

        initializeRecyclerView(binding.entriesList, binding.swipeLayout)

        // エントリリストの初期ロード
        if (viewModel.filteredEntries.value.isNullOrEmpty()) {
            lifecycleScope.launchWhenResumed {
                runCatching { viewModel.reloadLists() }
                    .onFailure { onErrorRefreshEntries(it) }
            }
        }

        updateEntryActionFlow
            .onEach { updateBookmark(it.entry, it.entry.bookmarkedData) }
            .launchIn(lifecycleScope)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()

        setEntriesAdapterListeners()

        binding.entriesList.adapter.alsoAs<EntriesAdapter> {
            it.onResume()
        }
    }

    /** エントリ項目用のリスナを設定する */
    private fun setEntriesAdapterListeners() {
        val adapter = binding.entriesList.adapter as? EntriesAdapter ?: return

        adapter.multipleClickDuration = activityViewModel.entryMultipleClickDuration

        adapter.setOnItemClickedListener { entry ->
            viewModel.onClickEntry(entriesActivity, entry, childFragmentManager)
        }

        adapter.setOnItemMultipleClickedListener { entry, _ ->
            viewModel.onMultipleClickEntry(entriesActivity, entry, childFragmentManager)
        }

        adapter.setOnItemLongClickedListener { entry ->
            viewModel.onLongClickEntry(entriesActivity, entry, childFragmentManager)
            true
        }

        adapter.setOnItemEdgeClickedListener { entry ->
            viewModel.onClickEntryEdge(entriesActivity, entry, childFragmentManager)
        }

        adapter.setOnItemEdgeMultipleClickedListener { entry, _ ->
            viewModel.onMultipleClickEntryEdge(entriesActivity, entry, childFragmentManager)
        }

        adapter.setOnItemEdgeLongClickedListener { entry ->
            viewModel.onLongClickEntryEdge(entriesActivity, entry, childFragmentManager)
            true
        }

        // コメント部分クリック時の挙動
        adapter.setOnCommentClickedListener { entry, bookmark ->
            viewModel.onClickComment(entriesActivity, entry, bookmark)
        }

        adapter.setOnCommentLongClickedListener { entry, bookmark ->
            viewModel.onLongClickComment(entry, bookmark, childFragmentManager)
        }
    }

    /** エントリリストを再取得する */
    fun reload() {
        binding.entriesList.adapter.alsoAs<EntriesAdapter> {
            it.clearEntries {
                lifecycleScope.launchWhenResumed {
                    runCatching { viewModel.reloadLists() }
                        .onFailure { e -> onErrorRefreshEntries(e) }
                }
            }
        }
    }

    /** リストを再構成する(取得はしない) */
    fun refreshList() {
        viewModel.viewModelScope.launch {
            viewModel.filter()
        }
    }

    /** エントリに付けたブクマを削除 */
    fun removeBookmark(entry: Entry) {
        viewModel.viewModelScope.launch {
            viewModel.deleteBookmark(entry)
        }
    }

    /** エントリに付けたブクマを更新する */
    fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult?) {
        viewModel.viewModelScope.launch {
            viewModel.updateBookmark(entry, bookmarkResult)
        }
    }

    /** 非表示対象のエントリ一覧 */
    fun openExcludedEntriesDialog() {
        ExcludedEntriesDialog.createInstance(
            viewModel.excludedEntries.value.orEmpty()
                .map {
                    ExcludedEntry(it, activityViewModel.repository.readEntryIds.value.contains(it.id))
                }
        ).show(childFragmentManager, null)
    }

    /** リストを上端までスクロールする */
    override fun scrollToTop() {
        binding.entriesList.scrollToPosition(0)
    }
}

