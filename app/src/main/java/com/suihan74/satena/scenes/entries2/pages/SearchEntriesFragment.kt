package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.observe
import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabAdapter
import com.suihan74.utilities.*
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
                    val root = this@SearchEntriesFragment.view

                    (root?.entries_tab_pager?.adapter as? EntriesTabAdapter)?.run {
                        refreshLists()
                    }

                    return (!query.isNullOrBlank()).also {
                        if (it) requireActivity().hideSoftInputMethod(root?.contentLayout)
                    }
                }
            })

            // 常に開いた状態にしておく
            setIconifiedByDefault(false)
            isIconified = false

            // 横幅を最大化
            stretchWidth(requireActivity())
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

            viewModel.searchType.observe(viewLifecycleOwner) {
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
            }
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
