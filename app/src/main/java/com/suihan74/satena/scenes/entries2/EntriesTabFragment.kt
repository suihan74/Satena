package com.suihan74.satena.scenes.entries2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentEntriesTab2Binding
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.fragment_entries_tab2.view.*

class EntriesTabFragment : Fragment() {
    companion object {
        fun createInstance(category: Category, tabPosition: Int = 0) = EntriesTabFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_CATEGORY, category.ordinal)
                putInt(ARG_TAB_POSITION, tabPosition)
            }
        }

        private const val ARG_CATEGORY = "ARG_CATEGORY"
        private const val ARG_TAB_POSITION = "ARG_TAB_POSITION"
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

        activityViewModel = ViewModelProvider(requireActivity())[EntriesViewModel::class.java]

        if (savedInstanceState == null) {
            val arguments = requireArguments()
            val category = Category.fromInt(arguments.getInt(ARG_CATEGORY))
            val tabPosition = arguments.getInt(ARG_TAB_POSITION, 0)

            val factory = EntriesTabFragmentViewModel.Factory(
                activityViewModel.repository,
                category,
                tabPosition
            )
            viewModel = ViewModelProvider(this, factory)[EntriesTabFragmentViewModel::class.java]
            viewModel.init(onErrorRefreshEntries)
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
            lifecycleOwner = this@EntriesTabFragment
            vm = viewModel
        }
        this.binding = binding

        val view = binding.root
        val context = requireContext()

        // エントリリストの設定
        view.entries_list.apply {
            val entriesAdapter = EntriesAdapter().apply {
                // TODO: クリック時の挙動をカスタマイズ可能にする
                setOnItemClickedListener { entry ->
                    val intent = Intent(context, BookmarksActivity::class.java).apply {
                        putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
                    }
                    startActivity(intent)
                }

                setOnItemLongClickedListener { entry ->
                    // TODO: 長押し時の挙動をカスタマイズ可能にする
                    true
                }

                // コメント部分クリック時の挙動
                setOnCommentClickedListener { entry, bookmark ->
                    val intent = Intent(context, BookmarksActivity::class.java).apply {
                        putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
                        putExtra(BookmarksActivity.EXTRA_TARGET_USER, bookmark.user)
                    }
                    startActivity(intent)
                }
            }

            adapter = entriesAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(
                DividerItemDecorator(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.recycler_view_item_divider
                    )!!
                )
            )
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

