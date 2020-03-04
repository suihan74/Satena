package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.hatenaLib.EntriesType
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentEntriesTab2Binding
import com.suihan74.satena.models.Category
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.fragment_entries_tab2.view.*

class EntriesTabFragment : Fragment() {
    companion object {
        fun createInstance() = EntriesTabFragment()
    }

    /** EntriesActivityのViewModel */
    private lateinit var activityViewModel : EntriesViewModel

    /** タブの表示内容に関するViewModel */
    private lateinit var viewModel : EntriesTabFragmentViewModel

    private var binding : FragmentEntriesTab2Binding? = null

    private val onErrorRefreshEntries: (Throwable)->Unit = { e ->
        Log.e("error", Log.getStackTraceString(e))
        activity?.showToast("リスト更新失敗")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProviders.of(requireActivity())[EntriesViewModel::class.java]

        if (savedInstanceState == null) {
            val factory = EntriesTabFragmentViewModel.Factory(
                activityViewModel.repository,
                Category.All,
                EntriesType.Hot
            )
            viewModel = ViewModelProviders.of(this, factory)[EntriesTabFragmentViewModel::class.java]
            viewModel.init(onErrorRefreshEntries)
        }
        else {
            viewModel = ViewModelProviders.of(this)[EntriesTabFragmentViewModel::class.java]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentEntriesTab2Binding>(inflater, R.layout.fragment_entries_tab2, container, false).apply {
            lifecycleOwner = this@EntriesTabFragment
            vm = viewModel
        }
        this.binding = binding

        val view = binding.root
        val context = requireContext()

        // エントリリストの設定
        view.entries_list.apply {
            val entriesAdapter = EntriesAdapter()
            adapter = entriesAdapter
            addItemDecoration(
                DividerItemDecorator(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.recycler_view_item_divider
                    )!!
                )
            )
            layoutManager = LinearLayoutManager(context)
        }

        // 引っ張って更新
        view.swipe_layout.apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(context.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(context.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                viewModel.init(onErrorRefreshEntries).invokeOnCompletion {
                    this.isRefreshing = false
                }
            }
        }

        return view
    }

    /** リストを上端までスクロールする */
    fun scrollToTop() {
        binding?.entriesList?.scrollToPosition(0)
    }
}

