package com.suihan74.satena.scenes.entries2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.NetworkReceiver
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentEntriesTab2Binding
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.entries2.dialog.EntryMenuDialog
import com.suihan74.satena.scenes.entries2.dialog.EntryMenuDialogListeners
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.fragment_entries_tab2.*
import kotlinx.android.synthetic.main.fragment_entries_tab2.view.*

abstract class EntriesTabFragmentBase : Fragment(), ScrollableToTop {
    companion object {
        /** このタブを表示しているEntriesFragmentのID */
        const val ARG_FRAGMENT_VIEW_MODEL_KEY = "ARG_FRAGMENT_VIEW_MODEL_KEY"

        /** このタブで表示するエントリのカテゴリ */
        const val ARG_CATEGORY = "ARG_CATEGORY"

        /** このタブの表示位置 */
        const val ARG_TAB_POSITION = "ARG_TAB_POSITION"
    }

    /** EntriesActivityのViewModel */
    protected val activityViewModel : EntriesViewModel by lazy {
        val activity = requireActivity() as EntriesActivity
        activity.viewModel
    }

    /** タブの表示内容に関するViewModel */
    protected val viewModel : EntriesTabFragmentViewModel by lazy {
        provideViewModel(this) {
            val arguments = requireArguments()
            val category = arguments.getEnum<Category>(ARG_CATEGORY)!!
            val tabPosition = arguments.getInt(ARG_TAB_POSITION, 0)

            EntriesTabFragmentViewModel(
                activityViewModel.repository,
                category,
                tabPosition
            )
        }
    }

    protected var binding : FragmentEntriesTab2Binding? = null

    /** 親のEntriesFragmentのViewModel */
    protected val parentViewModel : EntriesFragmentViewModel?
        get() =
            requireArguments().getString(ARG_FRAGMENT_VIEW_MODEL_KEY)?.let { key ->
                ViewModelProvider(requireActivity())[key, EntriesFragmentViewModel::class.java]
            }

    /** リスト更新失敗時に呼ばれる */
    protected val onErrorRefreshEntries: OnError = { e ->
        Log.e("error", Log.getStackTraceString(e))
        activity?.showToast(R.string.msg_update_entries_failed)
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
        val binding = DataBindingUtil.inflate<FragmentEntriesTab2Binding>(inflater, R.layout.fragment_entries_tab2, container, false).apply {
            lifecycleOwner = this@EntriesTabFragmentBase
            vm = viewModel
        }
        this.binding = binding

        // 通信状態の変更を監視
        // リスト未ロード状態なら再試行する
        var isNetworkReceiverInitialized = false
        SatenaApplication.instance.networkReceiver.state.observe(viewLifecycleOwner) { state ->
            if (!isNetworkReceiverInitialized) {
                isNetworkReceiverInitialized = true
                return@observe
            }

            if (state == NetworkReceiver.State.CONNECTED && viewModel.filteredEntries.value.isNullOrEmpty()) {
                viewModel.reloadLists(onError = onErrorRefreshEntries)
            }
        }

        return binding.root.also { view ->
            initializeRecyclerView(view.entries_list, view.swipe_layout)
        }
    }

    override fun onResume() {
        super.onResume()

        setEntriesAdapterListeners()

        // エントリリストの初期ロード
        if (viewModel.filteredEntries.value.isNullOrEmpty()) {
            viewModel.reloadLists(onError = onErrorRefreshEntries)
        }

        view?.entries_list?.adapter.alsoAs<EntriesAdapter> {
            it.onResume()
        }
    }

    /** エントリ項目用のリスナを設定する */
    private fun setEntriesAdapterListeners() {
        val adapter = entries_list.adapter as? EntriesAdapter ?: return

        // メニューアクション実行後に画面表示を更新する
        val listeners = EntryMenuDialogListeners().apply {
            onIgnoredEntry = { _ ->
                (activity as? EntriesActivity)?.refreshLists()
            }
            onDeletedBookmark = { entry ->
                (activity as? EntriesActivity)?.removeBookmark(entry)
            }
            onPostedBookmark = { entry, bookmarkResult ->
                (activity as? EntriesActivity)?.updateBookmark(entry, bookmarkResult)
            }
        }

        adapter.multipleClickDuration = activityViewModel.entryMultipleClickDuration

        adapter.setOnItemClickedListener { entry ->
            val context = requireContext()
            EntryMenuDialog.act(
                context,
                entry,
                activityViewModel.entryClickedAction,
                listeners,
                childFragmentManager,
                EntriesTabFragment.DIALOG_ENTRY_MENU
            )
        }

        adapter.setOnItemMultipleClickedListener { entry, _ ->
            val context = requireContext()
            EntryMenuDialog.act(
                context,
                entry,
                activityViewModel.entryMultipleClickedAction,
                listeners,
                childFragmentManager,
                EntriesTabFragment.DIALOG_ENTRY_MENU
            )
        }

        adapter.setOnItemLongClickedListener { entry ->
            val context = requireContext()
            EntryMenuDialog.act(
                context,
                entry,
                activityViewModel.entryLongClickedAction,
                listeners,
                childFragmentManager,
                EntriesTabFragment.DIALOG_ENTRY_MENU
            )
            true
        }

        // コメント部分クリック時の挙動
        adapter.setOnCommentClickedListener { entry, bookmark ->
            val intent = Intent(context, BookmarksActivity::class.java).apply {
                putObjectExtra(BookmarksActivity.EXTRA_ENTRY, entry)
                putExtra(BookmarksActivity.EXTRA_TARGET_USER, bookmark.user)
            }
            startActivity(intent)
        }
    }

    /** エントリリストを再取得する */
    fun reload() {
        view?.entries_list?.adapter.alsoAs<EntriesAdapter> {
            it.clearEntries {
                viewModel.reloadLists(onError = onErrorRefreshEntries)
            }
        }
    }

    /** リストを再構成する(取得はしない) */
    fun refreshList() {
        viewModel.filter()
    }

    /** エントリに付けたブクマを削除 */
    fun removeBookmark(entry: Entry) {
        viewModel.deleteBookmark(entry)
    }

    /** エントリに付けたブクマを更新する */
    fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult) {
        viewModel.updateBookmark(entry, bookmarkResult)
    }

    /** リストを上端までスクロールする */
    override fun scrollToTop() {
        binding?.entriesList?.scrollToPosition(0)
    }
}

