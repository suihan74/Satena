package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabAdapter
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.utilities.getEnum
import com.suihan74.utilities.hideSoftInputMethod
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments
import kotlinx.android.synthetic.main.fragment_entries2.view.*

class SearchEntriesFragment : TwinTabsEntriesFragment(), AlertDialogFragment.Listener {
    companion object {
        fun createInstance() = SearchEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.Search)
        }

        fun createInstance(query: String, searchType: SearchType) = SearchEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.Search)
            putString(ARG_SEARCH_QUERY, query)
            putEnum(ARG_SEARCH_TYPE, searchType)
        }

        /** 検索クエリ初期値 */
        private const val ARG_SEARCH_QUERY = "ARG_SEARCH_QUERY"
        /** 検索タイプ初期値 */
        private const val ARG_SEARCH_TYPE = "ARG_SEARCH_TYPE"

        /** 検索タイプを選択するためのダイアログ */
        private const val DIALOG_SEARCH_TYPE = "DIALOG_SEARCH_TYPE"
    }

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ): EntriesFragmentViewModel {
        val factory = SearchEntriesViewModel.Factory(repository)
        return ViewModelProvider(owner, factory)[viewModelKey, SearchEntriesViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val arguments = requireArguments()
            val viewModel = viewModel as SearchEntriesViewModel
            viewModel.searchQuery.value = arguments.getString(ARG_SEARCH_QUERY)
            viewModel.searchType.value = arguments.getEnum(ARG_SEARCH_TYPE, SearchType.Text)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_entries2, menu)

        val viewModel = viewModel as SearchEntriesViewModel

        // 検索クエリ入力ボックスの設定
        (menu.findItem(R.id.search_view)?.actionView as? SearchView)?.run {
            // クエリの設定
            val initialQuery = viewModel.searchQuery.value
            setQuery(initialQuery, false)
            queryHint = getString(R.string.search_query_hint)

            // クエリ文字列の変更を監視する
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.searchQuery.value = newText
                    return true
                }
                // 検索ボタン押下時にロードを行う
                override fun onQueryTextSubmit(query: String?): Boolean {
                    val tabPager = this@SearchEntriesFragment.view?.entries_tab_pager
                    val tabAdapter = tabPager?.adapter as? EntriesTabAdapter
                    if (tabPager != null && tabAdapter != null) {
                        (0 until tabAdapter.count).forEach { idx ->
                            val instance = tabAdapter.instantiateItem(tabPager, idx) as? EntriesTabFragmentBase
                            instance?.refresh()
                        }
                    }

                    return (!query.isNullOrBlank()).also {
                        if (it) requireActivity().hideSoftInputMethod()
                    }
                }
            })

            // 常に開いた状態にしておく
            setIconifiedByDefault(false)
            isIconified = false

            // 横幅を最大化
            val dMetrics = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(dMetrics)
            val buttonSize = (64 * resources.displayMetrics.density).toInt()  // TEXT/TAGボタンのサイズ分だけ小さくしないとボタンが画面外に出てしまう
            maxWidth = dMetrics.widthPixels - buttonSize

            // 左端の余分なマージンを削るための設定
            arrayOf(
                androidx.appcompat.R.id.search_edit_frame,
                androidx.appcompat.R.id.search_mag_icon
            ).forEach { targetId ->
                findViewById<View>(targetId)?.updateLayoutParams<LinearLayout.LayoutParams> {
                    marginStart = 0
                    leftMargin = 0
                }
            }
        }

        // 検索タイプ選択メニューの設定
        menu.findItem(R.id.search_type)?.let { item ->
            item.setOnMenuItemClickListener {
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.desc_search_type)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setItems(SearchType.values().map { it.name }.toTypedArray())
                    .show(childFragmentManager, DIALOG_SEARCH_TYPE)
                return@setOnMenuItemClickListener true
            }

            viewModel.searchType.observe(viewLifecycleOwner, Observer {
                item.title = it.name
                val context = requireContext()
                val iconId =
                    when (it) {
                        SearchType.Tag -> R.drawable.ic_tag
                        SearchType.Text -> R.drawable.ic_title
                        else -> throw RuntimeException()
                    }
                item.icon = ContextCompat.getDrawable(context, iconId)!!.apply {
                    setTint(context.getColor(R.color.colorPrimaryText))
                }
            })
        }
    }

    override fun onSelectItem(dialog: AlertDialogFragment, which: Int) {
        val viewModel = viewModel as SearchEntriesViewModel

        when (dialog.tag) {
            DIALOG_SEARCH_TYPE -> {
                viewModel.searchType.value = SearchType.values()[which]
            }
        }
    }
}