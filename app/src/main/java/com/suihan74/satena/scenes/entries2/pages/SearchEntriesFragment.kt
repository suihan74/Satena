package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.view.updateLayoutParams
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
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments
import kotlinx.android.synthetic.main.fragment_entries2.view.*

class SearchEntriesFragment : TwinTabsEntriesFragment(), AlertDialogFragment.Listener {
    companion object {
        fun createInstance() = SearchEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.Search)
        }

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
            val initialQuery = viewModel.searchQuery.value
            setQuery(initialQuery, !initialQuery.isNullOrBlank())
            queryHint = "検索クエリ"

            // クエリ文字列の変更を監視する
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.searchQuery.value = newText
                    return true
                }
                // 検索ボタン押下時にロードを行う
                override fun onQueryTextSubmit(query: String?): Boolean {
                    val tabPager = this@SearchEntriesFragment.view?.entries_tab_pager ?: return false
                    val tabAdapter = tabPager.adapter as? EntriesTabAdapter ?: return false
                    (0 until tabAdapter.count).forEach { idx ->
                        val instance = tabAdapter.instantiateItem(tabPager, idx) as? EntriesTabFragmentBase
                        instance?.refresh()
                    }

                    return !query.isNullOrBlank()
                }
            })

            // 常に開いた状態にしておく
            setIconifiedByDefault(false)
            isIconified = false

            if (!initialQuery.isNullOrBlank()) {
                // TODO: クエリが入力されている状態では画面復元時などにIMEを表示しないようにする
            }

            // 以下、左端のマージンを削るための設定

            findViewById<View>(androidx.appcompat.R.id.search_edit_frame)
                ?.updateLayoutParams<LinearLayout.LayoutParams> {
                    marginStart = 0
                    leftMargin = 0
                }

            findViewById<View>(androidx.appcompat.R.id.search_mag_icon)
                ?.updateLayoutParams<LinearLayout.LayoutParams> {
                    marginStart = 0
                    leftMargin = 0
                }
        }

        // 検索タイプ選択メニューの設定
        menu.findItem(R.id.search_type)?.run {
            setOnMenuItemClickListener {
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.desc_search_type)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setItems(SearchType.values().map { it.name }.toTypedArray())
                    .show(childFragmentManager, DIALOG_SEARCH_TYPE)

                return@setOnMenuItemClickListener true
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
