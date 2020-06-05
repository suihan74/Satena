package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentEntriesTab2Binding
import com.suihan74.satena.models.Category
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.getEnum
import com.suihan74.utilities.showToast
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
        val arguments = requireArguments()
        val category = arguments.getEnum<Category>(ARG_CATEGORY)!!
        val tabPosition = arguments.getInt(ARG_TAB_POSITION, 0)

        val factory = EntriesTabFragmentViewModel.Factory(
            activityViewModel.repository,
            category,
            tabPosition
        )

        ViewModelProvider(this, factory)[EntriesTabFragmentViewModel::class.java]
    }

    protected var binding : FragmentEntriesTab2Binding? = null

    /** 親のEntriesFragmentのViewModel */
    protected val parentViewModel : EntriesFragmentViewModel?
        get() =
            requireArguments().getString(ARG_FRAGMENT_VIEW_MODEL_KEY)?.let { key ->
                ViewModelProvider(requireActivity())[key, EntriesFragmentViewModel::class.java]
            }

    /** リスト更新失敗時に呼ばれる */
    protected val onErrorRefreshEntries: (Throwable)->Unit = { e ->
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

        // エントリリストの初期ロード
        if (savedInstanceState == null) {
            viewModel.refresh(onErrorRefreshEntries)
        }

        return binding.root.also { view ->
            initializeRecyclerView(view.entries_list, view.swipe_layout)
        }
    }

    /** エントリリストを再取得する */
    fun reload() {
        viewModel.refresh(onErrorRefreshEntries)
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

