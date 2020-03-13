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
        @JvmStatic
        protected val ARG_FRAGMENT_VIEW_MODEL_KEY = "ARG_FRAGMENT_VIEW_MODEL_KEY"

        /** このタブで表示するエントリのカテゴリ */
        @JvmStatic
        protected val ARG_CATEGORY = "ARG_CATEGORY"

        /** このタブの表示位置 */
        @JvmStatic
        protected val ARG_TAB_POSITION = "ARG_TAB_POSITION"
    }

    /** EntriesActivityのViewModel */
    protected lateinit var activityViewModel : EntriesViewModel

    /** タブの表示内容に関するViewModel */
    protected lateinit var viewModel : EntriesTabFragmentViewModel

    protected var binding : FragmentEntriesTab2Binding? = null

    /** リスト更新失敗時に呼ばれる */
    protected val onErrorRefreshEntries: (Throwable)->Unit = { e ->
        Log.e("error", Log.getStackTraceString(e))
        activity?.showToast(R.string.msg_update_entries_failed)
    }

    /**
     * コンテンツを表示するRecyclerViewを設定する
     */
    abstract fun initializeRecyclerView(entriesList: RecyclerView, swipeLayout: SwipeRefreshLayout)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProvider(requireActivity())[EntriesViewModel::class.java]

        if (savedInstanceState == null) {
            val arguments = requireArguments()
            val category = arguments.getEnum<Category>(ARG_CATEGORY)!!
            val tabPosition = arguments.getInt(ARG_TAB_POSITION, 0)

            val factory = EntriesTabFragmentViewModel.Factory(
                activityViewModel.repository,
                category,
                tabPosition
            )
            viewModel = ViewModelProvider(this, factory)[EntriesTabFragmentViewModel::class.java]
            viewModel.refresh(onErrorRefreshEntries)
        }
        else {
            viewModel = ViewModelProvider(this)[EntriesTabFragmentViewModel::class.java]
        }
    }

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

        return binding.root.also { view ->
            initializeRecyclerView(view.entries_list, view.swipe_layout)
        }
    }

    /** リストを上端までスクロールする */
    override fun scrollToTop() {
        binding?.entriesList?.scrollToPosition(0)
    }
}
