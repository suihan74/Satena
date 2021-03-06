package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.tabs.TabLayout
import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.provideViewModel
import com.suihan74.utilities.showAllowingStateLoss

class SearchEntriesFragment : MultipleTabsEntriesFragment() {
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

    // ------ //

    private val searchViewModel : SearchEntriesViewModel
        get() = viewModel as SearchEntriesViewModel

    // ------ //

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ) = provideViewModel(owner, viewModelKey) {
        SearchEntriesViewModel(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val arguments = requireArguments()
            searchViewModel.searchQuery.value = arguments.getString(ARG_SEARCH_QUERY)
            searchViewModel.searchType.value = arguments.getEnum(ARG_SEARCH_TYPE, SearchType.Text)
        }
    }

    override fun updateActivityAppBar(
        activity: EntriesActivity,
        tabLayout: TabLayout,
        bottomAppBar: BottomAppBar?
    ): Boolean {
        val result = super.updateActivityAppBar(activity, tabLayout, bottomAppBar)

        if (bottomAppBar == null) {
            setHasOptionsMenu(true)
        }
        else {
            setHasOptionsMenu(false)
            activity.inflateExtraBottomMenu(R.menu.search_entries_bottom)
            initializeMenu(bottomAppBar.menu, bottomAppBar)
        }

        if (!searchViewModel.searchQuery.value.isNullOrBlank()) {
            activity.alsoAs<EntriesActivity> {
                it.toolbar.subtitle = searchViewModel.searchQuery.value
            }
        }

        return result
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_entries, menu)
        initializeMenu(menu)
    }

    /** メニュー初期化処理 */
    private fun initializeMenu(menu: Menu, bottomAppBar: BottomAppBar? = null) {
        val viewModel = searchViewModel
        val activity = activity as EntriesActivity

        // 検索クエリ入力ボックスの設定
        val searchView =
            if (bottomAppBar == null) menu.findItem(R.id.search_view)?.actionView as? SearchView
            else activity.bottomSearchView?.also { searchView ->
                bottomAppBar.menu.findItem(R.id.search_view).setOnMenuItemClickListener {
                    searchView.setVisibility(searchView.visibility != View.VISIBLE)
                    true
                }
            }

        if (searchView != null) {
            initializeSearchView(searchView, viewModel, menu, bottomAppBar)
        }

        // 検索タイプ選択メニューの設定
        menu.findItem(R.id.search_type)?.let { item ->
            item.setOnMenuItemClickListener {
                AlertDialogFragment.Builder()
                    .setTitle(R.string.desc_search_type)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setItems(SearchType.values().map { it.textId }) { _, which ->
                        val prevValue = viewModel.searchType.value
                        viewModel.searchType.value = SearchType.fromOrdinal(which)

                        if (prevValue != viewModel.searchType.value) {
                            reloadLists()
                        }
                    }
                    .create()
                    .showAllowingStateLoss(childFragmentManager, DIALOG_SEARCH_TYPE)
                return@setOnMenuItemClickListener true
            }

            viewModel.searchType.observe(viewLifecycleOwner, {
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
                val text = getString(it.textId)
                item.title = text
                activity.alsoAs<EntriesActivity> { activity ->
                    activity.toolbar.title = text + getString(R.string.category_search)
                }
            })
        }
    }

    private fun initializeSearchView(
        searchView: SearchView,
        viewModel: SearchEntriesViewModel,
        menu: Menu,
        bottomAppBar: BottomAppBar?
    ) = searchView.run {
        val fragment = this@SearchEntriesFragment

        // 文字色をテーマに合わせて調整する
        if (bottomAppBar != null) {
            val color = context.getThemeColor(R.attr.textColor)
            val editText = findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
            editText?.setTextColor(color)
        }

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
                activity.alsoAs<EntriesActivity> { activity ->
                    activity.toolbar.subtitle = viewModel.searchQuery.value
                }

                reloadLists()

                requireActivity().hideSoftInputMethod(fragment.contentLayout)
                return true
            }
        })

        // 常に開いた状態にしておく
        setIconifiedByDefault(false)
        isIconified = false

        if (viewModel.searchQuery.value.isNullOrBlank()) {
            searchView.visibility = View.VISIBLE
        }
        else {
            // 初回遷移時などの未入力状態以外の場合は自動的にキーボードを表示しないようにする
            requireActivity().hideSoftInputMethod(fragment.contentLayout)
            clearFocus()
        }

        val magIcon = findViewById<View>(androidx.appcompat.R.id.search_mag_icon)
        magIcon?.background = null
        magIcon?.layoutParams = LinearLayout.LayoutParams(0, 0)

        // 横幅を最大化
        if (bottomAppBar == null) {
            stretchWidth(requireActivity(), menu)
        }
    }
}
