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
import com.suihan74.utilities.RecyclerViewScrollingUpdater
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

        private const val DIALOG_ENTRY_MENU = "entry_menu_dialog"
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
            lifecycleOwner = this@EntriesTabFragment
            vm = viewModel
        }
        this.binding = binding

        val view = binding.root
        val context = requireContext()

        // エントリリスト用のアダプタ
        val entriesAdapter = EntriesAdapter().apply {
            setOnItemClickedListener { entry ->
                MenuDialog.act(entry, activityViewModel.entryClickedAction, childFragmentManager, DIALOG_ENTRY_MENU)
            }

            setOnItemLongClickedListener { entry ->
                MenuDialog.act(entry, activityViewModel.entryLongClickedAction, childFragmentManager, DIALOG_ENTRY_MENU)
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

        // エントリリストの設定
        view.entries_list.apply {
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
                viewModel.refresh(onErrorRefreshEntries).invokeOnCompletion {
                    this.isRefreshing = false
                }
            }
        }

        // スクロールで追加ロード
        val scrollingUpdater = object : RecyclerViewScrollingUpdater(entriesAdapter) {
            override fun load() {
                entriesAdapter.showProgressBar()
                viewModel.loadAdditional(
                    onFinally = { loadCompleted() },
                    onError = { e ->
                        context.showToast(R.string.msg_get_entry_failed)
                        Log.e("loadAdditional", Log.getStackTraceString(e))
                    }
                )
            }
        }
        view.entries_list.addOnScrollListener(scrollingUpdater)

        return view
    }

    /** リストを上端までスクロールする */
    fun scrollToTop() {
        binding?.entriesList?.scrollToPosition(0)
    }
}

